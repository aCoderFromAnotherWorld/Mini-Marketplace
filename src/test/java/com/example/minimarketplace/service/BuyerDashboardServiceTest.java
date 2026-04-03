package com.example.minimarketplace.service;

import com.example.minimarketplace.entity.Product;
import com.example.minimarketplace.entity.Role;
import com.example.minimarketplace.entity.Sale;
import com.example.minimarketplace.entity.SellerRequest;
import com.example.minimarketplace.entity.User;
import com.example.minimarketplace.repository.ProductRepository;
import com.example.minimarketplace.repository.SaleRepository;
import com.example.minimarketplace.repository.SellerRequestRepository;
import com.example.minimarketplace.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuyerDashboardServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private SellerRequestRepository sellerRequestRepository;

    @InjectMocks
    private BuyerDashboardService buyerDashboardService;

    @Test
    void loadDashboardShouldAssembleBuyerAnalyticsAndLists() {
        User buyer = User.builder()
            .id(2L)
            .username("buyer1")
            .roles(Set.of(new Role(1L, "ROLE_BUYER")))
            .build();
        Product product = Product.builder()
            .id(9L)
            .name("Prompt Pack")
            .seller(User.builder().id(5L).username("seller1").build())
            .stock(4)
            .createdAt(LocalDateTime.now())
            .assetFilename("prompt-pack.zip")
            .build();
        Sale sale = Sale.builder().id(7L).product(product).seller(product.getSeller()).quantity(1).build();
        SellerRequest request = SellerRequest.builder().id(3L).build();

        when(userRepository.findByUsername("buyer1")).thenReturn(Optional.of(buyer));
        when(productRepository.findByStockGreaterThanOrderByCreatedAtDesc(0)).thenReturn(List.of(product));
        when(saleRepository.findTop10ByBuyerOrderBySoldAtDesc(buyer)).thenReturn(List.of(sale));
        when(sellerRequestRepository.findTopByUserOrderByRequestedAtDesc(buyer)).thenReturn(Optional.of(request));
        when(saleRepository.countByBuyer(buyer)).thenReturn(1L);
        when(saleRepository.totalUnitsPurchasedByBuyer(buyer)).thenReturn(2L);
        when(saleRepository.totalSpentByBuyer(buyer)).thenReturn(new BigDecimal("19.98"));
        when(saleRepository.countDistinctSellersByBuyer(buyer)).thenReturn(1L);
        when(saleRepository.countDownloadableOrdersByBuyer(buyer)).thenReturn(1L);
        when(productRepository.countByStockGreaterThan(0)).thenReturn(8L);

        var result = buyerDashboardService.loadDashboard("buyer1", "prompt", "seller1", "featured");

        assertThat(result.buyer()).isEqualTo(buyer);
        assertThat(result.marketplacePicks()).containsExactly(product);
        assertThat(result.recentOrders()).containsExactly(sale);
        assertThat(result.latestSellerRequest()).isEqualTo(request);
        assertThat(result.sellerOptions()).containsExactly("seller1");
        assertThat(result.downloadReadyOrders()).containsExactly(sale);
        assertThat(result.analytics().totalSpent()).isEqualByComparingTo("19.98");
        assertThat(result.analytics().downloadsReady()).isEqualTo(1L);
    }

    @Test
    void loadOrdersShouldFilterToDownloadableMatches() {
        User buyer = User.builder().id(2L).username("buyer1").build();
        User seller = User.builder().id(5L).username("seller1").build();
        Product downloadable = Product.builder().name("Prompt Pack").description("Bundle").assetFilename("bundle.zip").build();
        Product plain = Product.builder().name("Email Course").description("No file").build();
        Sale match = Sale.builder().id(1L).buyer(buyer).seller(seller).product(downloadable).build();
        Sale nonDownload = Sale.builder().id(2L).buyer(buyer).seller(seller).product(plain).build();

        when(userRepository.findByUsername("buyer1")).thenReturn(Optional.of(buyer));
        when(saleRepository.findByBuyerOrderBySoldAtDesc(buyer)).thenReturn(List.of(match, nonDownload));

        var orders = buyerDashboardService.loadOrders("buyer1", "prompt", true);

        assertThat(orders).containsExactly(match);
    }

    @Test
    void purchaseProductShouldCreateSaleAndDecreaseStock() {
        User buyer = User.builder()
            .id(2L)
            .username("buyer1")
            .roles(Set.of(new Role(1L, "ROLE_BUYER")))
            .build();
        User seller = User.builder().id(5L).username("seller1").build();
        Product product = Product.builder()
            .id(9L)
            .seller(seller)
            .name("Prompt Pack")
            .price(new BigDecimal("9.99"))
            .stock(5)
            .build();

        when(userRepository.findByUsername("buyer1")).thenReturn(Optional.of(buyer));
        when(productRepository.findById(9L)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Sale created = buyerDashboardService.purchaseProduct("buyer1", 9L, 2);

        assertThat(product.getStock()).isEqualTo(3);
        assertThat(created.getBuyer()).isEqualTo(buyer);
        assertThat(created.getSeller()).isEqualTo(seller);
        assertThat(created.getQuantity()).isEqualTo(2);
        assertThat(created.getTotalAmount()).isEqualByComparingTo("19.98");

        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(saleRepository).save(captor.capture());
        assertThat(captor.getValue().getUnitPrice()).isEqualByComparingTo("9.99");
    }

    @Test
    void purchaseProductShouldRejectOwnProduct() {
        User buyerSeller = User.builder()
            .id(2L)
            .username("buyer1")
            .roles(Set.of(new Role(1L, "ROLE_BUYER"), new Role(2L, "ROLE_SELLER")))
            .build();
        Product product = Product.builder()
            .id(9L)
            .seller(buyerSeller)
            .price(new BigDecimal("9.99"))
            .stock(5)
            .build();

        when(userRepository.findByUsername("buyer1")).thenReturn(Optional.of(buyerSeller));
        when(productRepository.findById(9L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> buyerDashboardService.purchaseProduct("buyer1", 9L, 1))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("You cannot purchase your own product.");
    }

    @Test
    void getOrderAssetShouldReturnProductFileForBuyerOrder() {
        User buyer = User.builder().id(2L).username("buyer1").build();
        Product product = Product.builder()
            .id(9L)
            .assetFilename("bundle.zip")
            .assetContentType("application/zip")
            .assetData(new byte[]{1, 2, 3})
            .build();
        Sale sale = Sale.builder().id(4L).buyer(buyer).product(product).build();

        when(userRepository.findByUsername("buyer1")).thenReturn(Optional.of(buyer));
        when(saleRepository.findByIdAndBuyer(4L, buyer)).thenReturn(Optional.of(sale));

        var payload = buyerDashboardService.getOrderAsset("buyer1", 4L);

        assertThat(payload.filename()).isEqualTo("bundle.zip");
        assertThat(payload.contentType()).isEqualTo("application/zip");
        assertThat(payload.data()).containsExactly(1, 2, 3);
    }
}
