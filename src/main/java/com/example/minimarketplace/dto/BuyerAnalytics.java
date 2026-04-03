package com.example.minimarketplace.dto;

import java.math.BigDecimal;

public record BuyerAnalytics(
    long totalOrders,
    long totalUnitsPurchased,
    BigDecimal totalSpent,
    long sellersSupported,
    long downloadsReady,
    long availableProducts
) {
}
