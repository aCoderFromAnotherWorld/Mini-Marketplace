package com.example.minimarketplace.controller;

import com.example.minimarketplace.entity.User;
import com.example.minimarketplace.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.ui.ExtendedModelMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private HomeController homeController;

    @Test
    void homeShouldPopulateUserContextForAuthenticatedUser() {
        User user = User.builder().username("seller1").build();
        var auth = new TestingAuthenticationToken("seller1", "pw", "ROLE_SELLER");
        var model = new ExtendedModelMap();

        when(userService.findByUsername("seller1")).thenReturn(user);

        String view = homeController.home(auth, model);

        assertThat(view).isEqualTo("home");
        assertThat(model.getAttribute("username")).isEqualTo("seller1");
        assertThat(model.getAttribute("role")).isEqualTo("SELLER");
        assertThat(model.getAttribute("user")).isEqualTo(user);
    }

    @Test
    void searchShouldKeepQueryForGuestUsers() {
        var model = new ExtendedModelMap();

        String view = homeController.search("laptop", null, model);

        assertThat(view).isEqualTo("home");
        assertThat(model.getAttribute("searchQuery")).isEqualTo("laptop");
        assertThat(model.getAttribute("username")).isNull();
    }
}
