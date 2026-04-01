package com.example.minimarketplace.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyRevenuePoint(LocalDate day, BigDecimal revenue) {
}
