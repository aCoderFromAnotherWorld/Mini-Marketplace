package com.example.minimarketplace.controller;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SellerControllerTest {

    private final SellerController sellerController = new SellerController();

    @Test
    void dashboardShouldReturnSellerDashboardView() {
        assertThat(sellerController.dashboard()).isEqualTo("seller/dashboard");
    }
}
