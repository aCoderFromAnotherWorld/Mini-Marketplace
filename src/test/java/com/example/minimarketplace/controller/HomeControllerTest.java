package com.example.minimarketplace.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.ui.ExtendedModelMap;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class HomeControllerTest {

    @InjectMocks
    private HomeController homeController;

    @Test
    void homeShouldRedirectBuyerToDashboard() {
        var auth = new TestingAuthenticationToken("buyer1", "pw", "ROLE_BUYER");
        var model = new ExtendedModelMap();

        String view = homeController.home(auth, model);

        assertThat(view).isEqualTo("redirect:/buyer/dashboard");
    }

    @Test
    void searchShouldKeepQueryForGuestUsers() {
        var model = new ExtendedModelMap();

        String view = homeController.search("laptop", null, model);

        assertThat(view).isEqualTo("home");
        assertThat(model.getAttribute("searchQuery")).isEqualTo("laptop");
    }

    @Test
    void searchShouldRedirectBuyerToDashboardWithQuery() {
        var auth = new TestingAuthenticationToken("buyer1", "pw", "ROLE_BUYER");
        var model = new ExtendedModelMap();

        String view = homeController.search("digital planner", auth, model);

        assertThat(view).isEqualTo("redirect:/buyer/dashboard?q=digital%20planner");
        assertThat(model.getAttribute("searchQuery")).isNull();
    }
}
