package com.example.minimarketplace.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.assertj.core.api.Assertions.assertThat;

class HomeControllerTest {

    private final HomeController homeController = new HomeController();

    @Test
    void homeShouldRedirectGuestUsersToLogin() {
        String view = homeController.home(null);

        assertThat(view).isEqualTo("redirect:/auth/login");
    }

    @Test
    void homeShouldRedirectSellerUsersToSellerDashboard() {
        var auth = new TestingAuthenticationToken("seller1", "pw", "ROLE_SELLER");

        String view = homeController.home(auth);

        assertThat(view).isEqualTo("redirect:/seller/dashboard");
    }

    @Test
    void searchShouldRedirectBuyerUsersToDashboardWithQuery() {
        var auth = new TestingAuthenticationToken("buyer1", "pw", "ROLE_BUYER");
        var redirectAttributes = new RedirectAttributesModelMap();

        String view = homeController.search("laptop", auth, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/buyer/dashboard");
        assertThat(redirectAttributes.getAttribute("q")).isEqualTo("laptop");
    }
}
