package com.example.minimarketplace.controller;

import com.example.minimarketplace.entity.Product;
import com.example.minimarketplace.entity.User;
import com.example.minimarketplace.repository.ProductRepository;
import com.example.minimarketplace.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.ui.ExtendedModelMap;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class HomeControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private HomeController homeController;

    @Test
    void homeShouldRedirectBuyerToDashboard() {
        var auth = new TestingAuthenticationToken("buyer1", "pw", "ROLE_BUYER");
        var model = new ExtendedModelMap();
        Product product = Product.builder().id(1L).name("Keyboard Shortcuts Guide").price(BigDecimal.valueOf(9.99)).stock(5).build();

        when(userService.findByUsername("seller1")).thenReturn(user);
        when(productRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(product));

        String view = homeController.home(auth, model);

        assertThat(view).isEqualTo("home");
        assertThat(model.getAttribute("username")).isEqualTo("seller1");
        assertThat(model.getAttribute("role")).isEqualTo("SELLER");
        assertThat(model.getAttribute("user")).isEqualTo(user);
        assertThat(model.getAttribute("products")).isEqualTo(List.of(product));
    }

    @Test
    void searchShouldKeepQueryForGuestUsers() {
        var model = new ExtendedModelMap();
        Product product = Product.builder().id(2L).name("Laptop Mockup Kit").price(BigDecimal.valueOf(14.99)).stock(2).build();
        when(productRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrderByCreatedAtDesc("laptop", "laptop"))
            .thenReturn(List.of(product));

        String view = homeController.search("laptop", null, model);

        assertThat(view).isEqualTo("home");
        assertThat(model.getAttribute("searchQuery")).isEqualTo("laptop");
        assertThat(model.getAttribute("username")).isNull();
        assertThat(model.getAttribute("products")).isEqualTo(List.of(product));
    }

    @Test
    void productDetailsShouldLoadRequestedProduct() {
        var model = new ExtendedModelMap();
        Product product = Product.builder().id(99L).name("UI Kit").price(BigDecimal.valueOf(19.99)).stock(8).build();

        when(productRepository.findById(99L)).thenReturn(Optional.of(product));

        String view = homeController.productDetails(99L, null, model);

        assertThat(view).isEqualTo("products/detail");
        assertThat(model.getAttribute("product")).isEqualTo(product);
    }
}
