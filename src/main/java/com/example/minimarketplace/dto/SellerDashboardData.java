package com.example.minimarketplace.dto;

import com.example.minimarketplace.entity.Product;
import com.example.minimarketplace.entity.Sale;

import java.util.List;

public record SellerDashboardData(
    List<Product> products,
    List<Sale> recentSales,
    SellerAnalytics analytics,
    List<DailyRevenuePoint> dailyRevenue
) {
}
