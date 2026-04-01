package com.example.minimarketplace.controller;

import com.example.minimarketplace.dto.DailyRevenuePoint;
import com.example.minimarketplace.dto.ProductForm;
import com.example.minimarketplace.dto.SaleForm;
import com.example.minimarketplace.dto.SellerAnalytics;
import com.example.minimarketplace.dto.SellerDashboardData;
import com.example.minimarketplace.entity.Product;
import com.example.minimarketplace.service.SellerDashboardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SellerControllerTest {

    @Mock
    private SellerDashboardService sellerDashboardService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private SellerController sellerController;

    @Test
    void dashboardShouldReturnSellerDashboardViewWithData() {
        var model = new ExtendedModelMap();
        var product = Product.builder().id(5L).name("Starter Kit").build();
        var analytics = new SellerAnalytics(1, 2, 3, new BigDecimal("59.99"), "Starter Kit", 3);
        var daily = new DailyRevenuePoint(LocalDate.of(2026, 4, 2), new BigDecimal("59.99"));

        when(authentication.getName()).thenReturn("seller1");
        when(sellerDashboardService.loadDashboard("seller1"))
            .thenReturn(new SellerDashboardData(List.of(product), List.of(), analytics, List.of(daily)));

        assertThat(sellerController.dashboard(authentication, model)).isEqualTo("seller/dashboard");
        assertThat(model.getAttribute("products")).isEqualTo(List.of(product));
        assertThat(model.getAttribute("analytics")).isEqualTo(analytics);
        assertThat(model.getAttribute("dailyRevenue")).isEqualTo(List.of(daily));
        assertThat(model.getAttribute("newProductForm")).isInstanceOf(ProductForm.class);
        assertThat(model.getAttribute("saleForm")).isInstanceOf(SaleForm.class);
    }

    @Test
    void recordSaleShouldRedirectWithSuccessMessage() {
        var redirectAttributes = new RedirectAttributesModelMap();
        var form = new SaleForm(7L, 2);

        when(authentication.getName()).thenReturn("seller1");

        String view = sellerController.recordSale(form, new org.springframework.validation.BeanPropertyBindingResult(form, "saleForm"), authentication, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/seller/dashboard");
        assertThat(redirectAttributes.getFlashAttributes().get("successMsg")).isEqualTo("Sale recorded successfully.");
        verify(sellerDashboardService).recordSale("seller1", form);
    }

    @Test
    void createProductShouldRedirectWithValidationError() {
        var redirectAttributes = new RedirectAttributesModelMap();
        var form = new ProductForm("", "desc", new BigDecimal("10.00"), 1);
        var bindingResult = new org.springframework.validation.BeanPropertyBindingResult(form, "newProductForm");
        bindingResult.rejectValue("name", "NotBlank", "Product name is required.");

        String view = sellerController.createProduct(form, bindingResult, null, null, authentication, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/seller/dashboard");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMsg")).isEqualTo("Product name is required.");
    }

    @Test
    void productImageShouldReturnPayloadFromService() {
        when(authentication.getName()).thenReturn("seller1");
        when(sellerDashboardService.getProductImage("seller1", 8L))
            .thenReturn(new SellerDashboardService.ProductImagePayload(new byte[]{1, 2}, "image/png"));

        var response = sellerController.productImage(8L, authentication);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType()).hasToString("image/png");
        assertThat(response.getBody()).containsExactly(1, 2);
    }

    @Test
    void productAssetShouldReturnAttachmentPayload() {
        when(authentication.getName()).thenReturn("seller1");
        when(sellerDashboardService.getProductAsset("seller1", 9L))
            .thenReturn(new SellerDashboardService.ProductAssetPayload(new byte[]{9, 8, 7}, "application/pdf", "guide.pdf"));

        var response = sellerController.productAsset(9L, authentication);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("attachment");
        assertThat(response.getHeaders().getContentType()).hasToString("application/pdf");
        assertThat(response.getBody()).containsExactly(9, 8, 7);
    }
}
