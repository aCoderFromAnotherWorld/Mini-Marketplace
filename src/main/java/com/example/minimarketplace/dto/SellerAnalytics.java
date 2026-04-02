package com.example.minimarketplace.dto;

import java.math.BigDecimal;

public record SellerAnalytics(
    long totalProducts,
    long totalSales,
    long totalUnitsSold,
    BigDecimal totalRevenue,
    String topProductName,
    long topProductUnits
) {
}
