package com.example.minimarketplace.controller;

import com.example.minimarketplace.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuyerControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private BuyerController buyerController;

    @Test
    void ordersShouldReturnOrdersView() {
        assertThat(buyerController.orders()).isEqualTo("buyer/orders");
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
}
