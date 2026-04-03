package com.example.minimarketplace.dto;

import com.example.minimarketplace.entity.Product;
import com.example.minimarketplace.entity.Sale;
import com.example.minimarketplace.entity.SellerRequest;
import com.example.minimarketplace.entity.User;

import java.util.List;

public record BuyerDashboardData(
    User buyer,
    BuyerAnalytics analytics,
    List<Product> marketplacePicks,
    List<Sale> recentOrders,
    SellerRequest latestSellerRequest,
    List<String> sellerOptions,
    List<Sale> downloadReadyOrders
) {
}
