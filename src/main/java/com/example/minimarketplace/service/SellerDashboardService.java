package com.example.minimarketplace.service;

import com.example.minimarketplace.dto.DailyRevenuePoint;
import com.example.minimarketplace.dto.ProductForm;
import com.example.minimarketplace.dto.SaleForm;
import com.example.minimarketplace.dto.SellerAnalytics;
import com.example.minimarketplace.dto.SellerDashboardData;
import com.example.minimarketplace.entity.Product;
import com.example.minimarketplace.entity.Sale;
import com.example.minimarketplace.entity.User;
import com.example.minimarketplace.repository.ProductRepository;
import com.example.minimarketplace.repository.SaleRepository;
import com.example.minimarketplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class SellerDashboardService {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
        "image/jpeg", "image/png", "image/webp", "image/gif"
    );
    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
    private static final long MAX_ASSET_BYTES = 20L * 1024 * 1024;

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;

    @Transactional(readOnly = true)
    public SellerDashboardData loadDashboard(String username) {
        User seller = getSeller(username);

        List<Product> products = productRepository.findBySellerOrderByCreatedAtDesc(seller);
        List<Sale> recentSales = saleRepository.findTop10BySellerOrderBySoldAtDesc(seller);
        SellerAnalytics analytics = buildAnalytics(seller);
        List<DailyRevenuePoint> dailyRevenue = buildDailyRevenue(seller);

        return new SellerDashboardData(products, recentSales, analytics, dailyRevenue);
    }

    @Transactional
    public Product createProduct(String username, ProductForm form, MultipartFile imageFile, MultipartFile assetFile) {
        User seller = getSeller(username);

        Product product = Product.builder()
            .seller(seller)
            .name(form.getName().trim())
            .description(normalizeDescription(form.getDescription()))
            .price(scaleMoney(form.getPrice()))
            .stock(form.getStock())
            .build();

        applyImageUpload(product, imageFile);
        applyAssetUpload(product, assetFile);
        Product saved = productRepository.save(product);
        log.info("Seller '{}' created product '{}'", username, saved.getName());
        return saved;
    }

    @Transactional
    public Product updateProduct(String username, Long productId, ProductForm form, MultipartFile imageFile, MultipartFile assetFile) {
        User seller = getSeller(username);
        Product product = productRepository.findByIdAndSeller(productId, seller)
            .orElseThrow(() -> new RuntimeException("Product not found."));

        product.setName(form.getName().trim());
        product.setDescription(normalizeDescription(form.getDescription()));
        product.setPrice(scaleMoney(form.getPrice()));
        product.setStock(form.getStock());
        applyImageUpload(product, imageFile);
        applyAssetUpload(product, assetFile);

        Product saved = productRepository.save(product);
        log.info("Seller '{}' updated product #{}", username, productId);
        return saved;
    }

    @Transactional
    public void deleteProduct(String username, Long productId) {
        User seller = getSeller(username);
        Product product = productRepository.findByIdAndSeller(productId, seller)
            .orElseThrow(() -> new RuntimeException("Product not found."));

        long salesCount = saleRepository.countBySellerAndProductId(seller, productId);
        if (salesCount > 0) {
            throw new RuntimeException("Cannot delete a product that already has sales history.");
        }

        productRepository.delete(product);
        log.info("Seller '{}' deleted product #{}", username, productId);
    }

    @Transactional
    public Sale recordSale(String username, SaleForm form) {
        User seller = getSeller(username);
        Product product = productRepository.findByIdAndSeller(form.getProductId(), seller)
            .orElseThrow(() -> new RuntimeException("Product not found."));

        if (product.getStock() < form.getQuantity()) {
            throw new RuntimeException("Not enough stock. Available: " + product.getStock());
        }

        BigDecimal unitPrice = scaleMoney(product.getPrice());
        BigDecimal totalAmount = unitPrice
            .multiply(BigDecimal.valueOf(form.getQuantity()))
            .setScale(2, RoundingMode.HALF_UP);

        product.setStock(product.getStock() - form.getQuantity());
        productRepository.save(product);

        Sale sale = Sale.builder()
            .seller(seller)
            .product(product)
            .quantity(form.getQuantity())
            .unitPrice(unitPrice)
            .totalAmount(totalAmount)
            .build();

        Sale saved = saleRepository.save(sale);
        log.info("Seller '{}' recorded sale: product #{} qty {}", username, product.getId(), form.getQuantity());
        return saved;
    }

    @Transactional(readOnly = true)
    public ProductImagePayload getProductImage(String username, Long productId) {
        User seller = getSeller(username);
        Product product = productRepository.findByIdAndSeller(productId, seller)
            .orElseThrow(() -> new RuntimeException("Product not found."));

        if (product.getImageData() == null || product.getImageData().length == 0) {
            throw new RuntimeException("No image uploaded for this product.");
        }

        String contentType = Optional.ofNullable(product.getImageContentType())
            .filter(type -> !type.isBlank())
            .orElse("application/octet-stream");

        return new ProductImagePayload(product.getImageData(), contentType);
    }

    @Transactional(readOnly = true)
    public ProductAssetPayload getProductAsset(String username, Long productId) {
        User seller = getSeller(username);
        Product product = productRepository.findByIdAndSeller(productId, seller)
            .orElseThrow(() -> new RuntimeException("Product not found."));

        if (product.getAssetData() == null || product.getAssetData().length == 0) {
            throw new RuntimeException("No product file uploaded for this product.");
        }

        String contentType = Optional.ofNullable(product.getAssetContentType())
            .filter(type -> !type.isBlank())
            .orElse("application/octet-stream");
        String filename = Optional.ofNullable(product.getAssetFilename())
            .filter(name -> !name.isBlank())
            .orElse("product-file");

        return new ProductAssetPayload(product.getAssetData(), contentType, filename);
    }

    private SellerAnalytics buildAnalytics(User seller) {
        long totalProducts = productRepository.countBySeller(seller);
        long totalSales = saleRepository.countBySeller(seller);
        long totalUnitsSold = Optional.ofNullable(saleRepository.totalUnitsSoldBySeller(seller)).orElse(0L);
        BigDecimal totalRevenue = scaleMoney(saleRepository.totalRevenueBySeller(seller));

        var topProduct = saleRepository.findTopProductsBySeller(seller, PageRequest.of(0, 1)).stream().findFirst();
        String topProductName = topProduct.map(SaleRepository.TopProductProjection::getProductName).orElse("N/A");
        long topProductUnits = topProduct.map(p -> Optional.ofNullable(p.getUnits()).orElse(0L)).orElse(0L);

        return new SellerAnalytics(
            totalProducts,
            totalSales,
            totalUnitsSold,
            totalRevenue,
            topProductName,
            topProductUnits
        );
    }

    private List<DailyRevenuePoint> buildDailyRevenue(User seller) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(6);

        Map<LocalDate, BigDecimal> totalsByDay = new LinkedHashMap<>();
        for (int i = 0; i < 7; i++) {
            LocalDate day = startDate.plusDays(i);
            totalsByDay.put(day, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }

        List<Sale> sales = saleRepository.findBySellerAndSoldAtGreaterThanEqualOrderBySoldAtAsc(
            seller, startDate.atStartOfDay()
        );
        for (Sale sale : sales) {
            LocalDate day = sale.getSoldAt().toLocalDate();
            if (!totalsByDay.containsKey(day)) {
                continue;
            }
            BigDecimal current = totalsByDay.get(day);
            totalsByDay.put(day, current.add(scaleMoney(sale.getTotalAmount())));
        }

        return totalsByDay.entrySet().stream()
            .map(entry -> new DailyRevenuePoint(entry.getKey(), scaleMoney(entry.getValue())))
            .toList();
    }

    private void applyImageUpload(Product product, MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            return;
        }

        String contentType = Optional.ofNullable(imageFile.getContentType()).orElse("").toLowerCase(Locale.ROOT);
        if (!ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new RuntimeException("Only JPG, PNG, WEBP, and GIF images are allowed.");
        }
        if (imageFile.getSize() > MAX_IMAGE_BYTES) {
            throw new RuntimeException("Image size must be 5 MB or less.");
        }

        try {
            product.setImageFilename(imageFile.getOriginalFilename());
            product.setImageContentType(contentType);
            product.setImageData(imageFile.getBytes());
        } catch (Exception ex) {
            throw new RuntimeException("Failed to process uploaded file.", ex);
        }
    }

    private void applyAssetUpload(Product product, MultipartFile assetFile) {
        if (assetFile == null || assetFile.isEmpty()) {
            return;
        }

        if (assetFile.getSize() > MAX_ASSET_BYTES) {
            throw new RuntimeException("Product file size must be 20 MB or less.");
        }

        try {
            product.setAssetFilename(assetFile.getOriginalFilename());
            product.setAssetContentType(assetFile.getContentType());
            product.setAssetData(assetFile.getBytes());
        } catch (Exception ex) {
            throw new RuntimeException("Failed to process uploaded product file.", ex);
        }
    }

    private User getSeller(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
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
