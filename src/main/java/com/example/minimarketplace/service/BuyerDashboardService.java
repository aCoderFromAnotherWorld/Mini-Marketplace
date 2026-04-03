package com.example.minimarketplace.service;

import com.example.minimarketplace.dto.BuyerAnalytics;
import com.example.minimarketplace.dto.BuyerDashboardData;
import com.example.minimarketplace.entity.Product;
import com.example.minimarketplace.entity.Sale;
import com.example.minimarketplace.entity.SellerRequest;
import com.example.minimarketplace.entity.User;
import com.example.minimarketplace.repository.ProductRepository;
import com.example.minimarketplace.repository.SaleRepository;
import com.example.minimarketplace.repository.SellerRequestRepository;
import com.example.minimarketplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class BuyerDashboardService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;
    private final SellerRequestRepository sellerRequestRepository;

    @Transactional(readOnly = true)
    public BuyerDashboardData loadDashboard(String username, String query, String sellerFilter, String sort) {
        User buyer = getUser(username);

        List<Product> availableProducts = loadMarketplaceProducts(buyer);
        List<Product> marketplacePicks = filterProducts(availableProducts, query, sellerFilter, sort).stream()
            .limit(18)
            .toList();
        List<Sale> recentOrders = saleRepository.findTop10ByBuyerOrderBySoldAtDesc(buyer);
        List<Sale> downloadReadyOrders = recentOrders.stream()
            .filter(this::hasDownload)
            .limit(4)
            .toList();
        SellerRequest latestSellerRequest = sellerRequestRepository.findTopByUserOrderByRequestedAtDesc(buyer)
            .orElse(null);

        return new BuyerDashboardData(
            buyer,
            buildAnalytics(buyer),
            marketplacePicks,
            recentOrders,
            latestSellerRequest,
            extractSellerOptions(availableProducts),
            downloadReadyOrders
        );
    }

    @Transactional(readOnly = true)
    public List<Sale> loadOrders(String username, String query, boolean downloadsOnly) {
        User buyer = getUser(username);
        return saleRepository.findByBuyerOrderBySoldAtDesc(buyer).stream()
            .filter(order -> !downloadsOnly || hasDownload(order))
            .filter(order -> matchesOrderQuery(order, query))
            .toList();
    }

    @Transactional
    public Sale purchaseProduct(String username, Long productId, Integer quantity) {
        User buyer = getUser(username);
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found."));

        int normalizedQuantity = normalizeQuantity(quantity);
        if (product.getSeller() != null && product.getSeller().getId() != null
            && product.getSeller().getId().equals(buyer.getId())) {
            throw new RuntimeException("You cannot purchase your own product.");
        }
        if (product.getStock() == null || product.getStock() <= 0) {
            throw new RuntimeException("This product is currently out of stock.");
        }
        if (product.getStock() < normalizedQuantity) {
            throw new RuntimeException("Only " + product.getStock() + " item(s) are available right now.");
        }

        BigDecimal unitPrice = scaleMoney(product.getPrice());
        BigDecimal totalAmount = unitPrice
            .multiply(BigDecimal.valueOf(normalizedQuantity))
            .setScale(2, RoundingMode.HALF_UP);

        product.setStock(product.getStock() - normalizedQuantity);
        productRepository.save(product);

        Sale sale = Sale.builder()
            .seller(product.getSeller())
            .buyer(buyer)
            .product(product)
            .quantity(normalizedQuantity)
            .unitPrice(unitPrice)
            .totalAmount(totalAmount)
            .build();

        Sale saved = saleRepository.save(sale);
        log.info("Buyer '{}' purchased product #{} qty {}", username, productId, normalizedQuantity);
        return saved;
    }

    @Transactional(readOnly = true)
    public ProductImagePayload getProductImage(Long productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found."));

        if (product.getImageData() == null || product.getImageData().length == 0) {
            throw new RuntimeException("No image available for this product.");
        }

        String contentType = Optional.ofNullable(product.getImageContentType())
            .filter(type -> !type.isBlank())
            .orElse("application/octet-stream");

        return new ProductImagePayload(product.getImageData(), contentType);
    }

    @Transactional(readOnly = true)
    public ProductAssetPayload getOrderAsset(String username, Long saleId) {
        User buyer = getUser(username);
        Sale sale = saleRepository.findByIdAndBuyer(saleId, buyer)
            .orElseThrow(() -> new RuntimeException("Order not found."));
        Product product = sale.getProduct();

        if (product.getAssetData() == null || product.getAssetData().length == 0) {
            throw new RuntimeException("This product does not include a downloadable file.");
        }

        String contentType = Optional.ofNullable(product.getAssetContentType())
            .filter(type -> !type.isBlank())
            .orElse("application/octet-stream");
        String filename = Optional.ofNullable(product.getAssetFilename())
            .filter(name -> !name.isBlank())
            .orElse("product-file");

        return new ProductAssetPayload(product.getAssetData(), contentType, filename);
    }

    private List<Product> loadMarketplaceProducts(User buyer) {
        return productRepository.findByStockGreaterThanOrderByCreatedAtDesc(0).stream()
            .filter(product -> !isOwnProduct(product, buyer))
            .toList();
    }

    private List<Product> filterProducts(List<Product> products, String query, String sellerFilter, String sort) {
        Stream<Product> stream = products.stream()
            .filter(product -> matchesCatalogQuery(product, query))
            .filter(product -> matchesSellerFilter(product, sellerFilter));

        return stream.sorted(resolveProductComparator(sort)).toList();
    }

    private List<String> extractSellerOptions(List<Product> products) {
        return products.stream()
            .map(Product::getSeller)
            .filter(seller -> seller != null && seller.getUsername() != null && !seller.getUsername().isBlank())
            .map(User::getUsername)
            .distinct()
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
    }

    private BuyerAnalytics buildAnalytics(User buyer) {
        long totalOrders = saleRepository.countByBuyer(buyer);
        long totalUnitsPurchased = Optional.ofNullable(saleRepository.totalUnitsPurchasedByBuyer(buyer)).orElse(0L);
        BigDecimal totalSpent = scaleMoney(saleRepository.totalSpentByBuyer(buyer));
        long sellersSupported = saleRepository.countDistinctSellersByBuyer(buyer);
        long downloadsReady = saleRepository.countDownloadableOrdersByBuyer(buyer);
        long availableProducts = productRepository.countByStockGreaterThan(0);

        return new BuyerAnalytics(
            totalOrders,
            totalUnitsPurchased,
            totalSpent,
            sellersSupported,
            downloadsReady,
            availableProducts
        );
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    private Comparator<Product> resolveProductComparator(String sort) {
        return switch (normalizeSort(sort)) {
            case "newest" -> Comparator.comparing(Product::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed();
            case "priceAsc" -> Comparator.comparing(Product::getPrice, Comparator.nullsLast(BigDecimal::compareTo))
                .thenComparing(Product::getName, String.CASE_INSENSITIVE_ORDER);
            case "priceDesc" -> Comparator.comparing(Product::getPrice, Comparator.nullsLast(BigDecimal::compareTo)).reversed()
                .thenComparing(Product::getName, String.CASE_INSENSITIVE_ORDER);
            case "name" -> Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER);
            default -> Comparator
                .comparing((Product product) -> !hasAsset(product))
                .thenComparing(Product::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Product::getPrice, Comparator.nullsLast(BigDecimal::compareTo));
        };
    }

    private boolean matchesCatalogQuery(Product product, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);
        return containsIgnoreCase(product.getName(), normalizedQuery)
            || containsIgnoreCase(product.getDescription(), normalizedQuery)
            || containsIgnoreCase(product.getSeller() != null ? product.getSeller().getUsername() : null, normalizedQuery);
    }

    private boolean matchesSellerFilter(Product product, String sellerFilter) {
        if (sellerFilter == null || sellerFilter.isBlank()) {
            return true;
        }
        String username = product.getSeller() != null ? product.getSeller().getUsername() : null;
        return username != null && username.equalsIgnoreCase(sellerFilter.trim());
    }

    private boolean matchesOrderQuery(Sale order, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);
        return containsIgnoreCase(order.getProduct() != null ? order.getProduct().getName() : null, normalizedQuery)
            || containsIgnoreCase(order.getProduct() != null ? order.getProduct().getDescription() : null, normalizedQuery)
            || containsIgnoreCase(order.getSeller() != null ? order.getSeller().getUsername() : null, normalizedQuery);
    }

    private boolean containsIgnoreCase(String value, String normalizedQuery) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(normalizedQuery);
    }

    private String normalizeSort(String sort) {
        if (sort == null) {
            return "featured";
        }
        return switch (sort) {
            case "newest", "priceAsc", "priceDesc", "name" -> sort;
            default -> "featured";
        };
    }

    private boolean hasAsset(Product product) {
        return product != null && product.getAssetFilename() != null && !product.getAssetFilename().isBlank();
    }

    private boolean hasDownload(Sale sale) {
        return sale != null && hasAsset(sale.getProduct());
    }

    private boolean isOwnProduct(Product product, User buyer) {
        return product.getSeller() != null
            && product.getSeller().getId() != null
            && buyer.getId() != null
            && product.getSeller().getId().equals(buyer.getId());
    }

    private int normalizeQuantity(Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new RuntimeException("Quantity must be at least 1.");
        }
        return quantity;
    }

    private BigDecimal scaleMoney(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public record ProductImagePayload(byte[] data, String contentType) {
    }

    public record ProductAssetPayload(byte[] data, String contentType, String filename) {
    }
}
