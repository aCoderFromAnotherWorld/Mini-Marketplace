package com.example.minimarketplace.controller;

import com.example.minimarketplace.dto.BuyerAnalytics;
import com.example.minimarketplace.dto.BuyerDashboardData;
import com.example.minimarketplace.entity.Product;
import com.example.minimarketplace.entity.Sale;
import com.example.minimarketplace.entity.User;
import com.example.minimarketplace.service.BuyerDashboardService;
import com.example.minimarketplace.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuyerControllerTest {

    @Mock
    private BuyerDashboardService buyerDashboardService;

    @Mock
    private UserService userService;

    @Mock
    private Authentication authentication;

    @Mock
    private HttpSession session;

    @InjectMocks
    private BuyerController buyerController;

    @Test
    void dashboardShouldPopulateBuyerDataAndReturnView() {
        var model = new ExtendedModelMap();
        var buyer = User.builder().username("buyer1").build();
        var analytics = new BuyerAnalytics(2, 3, new BigDecimal("29.98"), 1, 2, 5);
        var product = Product.builder().id(7L).name("Prompt Pack").build();
        var order = Sale.builder().id(15L).build();
        var data = new BuyerDashboardData(buyer, analytics, List.of(product), List.of(order), null, List.of("seller1"), List.of(order));

        when(authentication.getName()).thenReturn("buyer1");
        when(buyerDashboardService.loadDashboard("buyer1", "prompt", "seller1", "priceAsc")).thenReturn(data);

        String view = buyerController.dashboard("prompt", "seller1", "priceAsc", "list", authentication, model);

        assertThat(view).isEqualTo("buyer/dashboard");
        assertThat(model.getAttribute("buyer")).isEqualTo(buyer);
        assertThat(model.getAttribute("analytics")).isEqualTo(analytics);
        assertThat(model.getAttribute("marketplacePicks")).isEqualTo(List.of(product));
        assertThat(model.getAttribute("catalogView")).isEqualTo("list");
        assertThat(model.getAttribute("sellerOptions")).isEqualTo(List.of("seller1"));
    }

    @Test
    void ordersShouldReturnOrdersViewWithOrderList() {
        var model = new ExtendedModelMap();
        var buyer = User.builder().username("buyer1").build();
        var analytics = new BuyerAnalytics(1, 1, new BigDecimal("9.99"), 1, 1, 3);
        var order = Sale.builder().id(11L).quantity(1).build();
        var product = Product.builder().id(7L).name("Prompt Pack").build();
        var data = new BuyerDashboardData(buyer, analytics, List.of(product), List.of(order), null, List.of("seller1"), List.of(order));

        when(authentication.getName()).thenReturn("buyer1");
        when(buyerDashboardService.loadDashboard("buyer1", "", "", "featured")).thenReturn(data);
        when(buyerDashboardService.loadOrders("buyer1", "prompt", true)).thenReturn(List.of(order));

        String view = buyerController.orders("prompt", true, authentication, model);

        assertThat(view).isEqualTo("buyer/orders");
        assertThat(model.getAttribute("orders")).isEqualTo(List.of(order));
        assertThat(model.getAttribute("recentOrders")).isEqualTo(List.of(order));
        assertThat(model.getAttribute("orderQuery")).isEqualTo("prompt");
        assertThat(model.getAttribute("downloadsOnly")).isEqualTo(true);
    }

    @Test
    void purchaseShouldRedirectToOrdersAndSetSuccessMessage() {
        var redirectAttributes = new RedirectAttributesModelMap();

        when(authentication.getName()).thenReturn("buyer1");

        String view = buyerController.purchase(9L, 2, "/buyer/orders", authentication, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/buyer/orders");
        assertThat(redirectAttributes.getFlashAttributes().get("successMsg"))
            .isEqualTo("Purchase completed. Your order is now in your library.");
        verify(buyerDashboardService).purchaseProduct("buyer1", 9L, 2);
    }

    @Test
    void purchaseShouldSanitizeUnexpectedReturnRoute() {
        var redirectAttributes = new RedirectAttributesModelMap();

        when(authentication.getName()).thenReturn("buyer1");
        doThrow(new RuntimeException("Product not found."))
            .when(buyerDashboardService).purchaseProduct("buyer1", 99L, 1);

        String view = buyerController.purchase(99L, 1, "/admin/dashboard", authentication, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/buyer/dashboard");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMsg"))
            .isEqualTo("Product not found.");
    }

    @Test
    void submitSellerRequestShouldAddSuccessFlashMessage() {
        var redirectAttributes = new RedirectAttributesModelMap();

        when(authentication.getName()).thenReturn("buyer1");

        String view = buyerController.submitSellerRequest("Ready to sell", authentication, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/buyer/seller-request");
        assertThat(redirectAttributes.getFlashAttributes().get("successMsg"))
            .isEqualTo("Request submitted! Admin will review it soon.");
        verify(userService).requestSellerRole("buyer1", "Ready to sell");
    }

    @Test
    void submitSellerRequestShouldAddErrorFlashMessageWhenServiceFails() {
        var redirectAttributes = new RedirectAttributesModelMap();

        when(authentication.getName()).thenReturn("buyer1");
        doThrow(new RuntimeException("You already have a pending request."))
            .when(userService).requestSellerRole("buyer1", "Ready to sell");

        String view = buyerController.submitSellerRequest("Ready to sell", authentication, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/buyer/seller-request");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMsg"))
            .isEqualTo("You already have a pending request.");
    }

    @Test
    void addToCartShouldStoreQuantityAndRedirect() {
        var redirectAttributes = new RedirectAttributesModelMap();
        Map<Long, Integer> cart = new LinkedHashMap<>();
        when(session.getAttribute("buyerCart")).thenReturn(cart);

        String view = buyerController.addToCart(9L, 2, "/buyer/cart", session, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/buyer/cart");
        assertThat(redirectAttributes.getFlashAttributes().get("successMsg"))
            .isEqualTo("Added to cart.");
        verify(session, atLeastOnce()).setAttribute("buyerCart", Map.of(9L, 2));
    }

    @Test
    void checkoutCartShouldClearSessionAndSetSuccessMessage() {
        var redirectAttributes = new RedirectAttributesModelMap();
        Map<Long, Integer> cart = new LinkedHashMap<>();
        cart.put(7L, 1);

        when(authentication.getName()).thenReturn("buyer1");
        when(session.getAttribute("buyerCart")).thenReturn(cart);
        when(buyerDashboardService.checkoutCart("buyer1", Map.of(7L, 1))).thenReturn(1);

        String view = buyerController.checkoutCart(authentication, session, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/buyer/cart");
        assertThat(redirectAttributes.getFlashAttributes().get("successMsg"))
            .isEqualTo("Checkout complete for 1 cart item(s).");
        verify(buyerDashboardService).checkoutCart("buyer1", Map.of(7L, 1));
    }

    @Test
    void updateAddressShouldCallUserServiceAndRedirectToAccount() {
        var redirectAttributes = new RedirectAttributesModelMap();
        when(authentication.getName()).thenReturn("buyer1");

        String view = buyerController.updateAddress("Dhaka, Bangladesh", authentication, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/buyer/account");
        assertThat(redirectAttributes.getFlashAttributes().get("successMsg"))
            .isEqualTo("Shipping address updated.");
        verify(userService).updateShippingAddress("buyer1", "Dhaka, Bangladesh");
    }

    @Test
    void downloadOrderAssetShouldReturnAttachmentPayload() {
        when(authentication.getName()).thenReturn("buyer1");
        when(buyerDashboardService.getOrderAsset("buyer1", 5L))
            .thenReturn(new BuyerDashboardService.ProductAssetPayload(new byte[]{4, 2}, "application/pdf", "guide.pdf"));

        var response = buyerController.downloadOrderAsset(5L, authentication);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("attachment");
        assertThat(response.getHeaders().getContentType()).hasToString("application/pdf");
        assertThat(response.getBody()).containsExactly(4, 2);
    }
}
