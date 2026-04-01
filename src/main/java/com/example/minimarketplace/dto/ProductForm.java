package com.example.minimarketplace.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductForm {

    @NotBlank(message = "Product name is required.")
    @Size(max = 120, message = "Product name must be at most 120 characters.")
    private String name;

    @Size(max = 1000, message = "Description must be at most 1000 characters.")
    private String description;

    @NotNull(message = "Price is required.")
    @DecimalMin(value = "0.01", message = "Price must be at least 0.01.")
    @Digits(integer = 10, fraction = 2, message = "Price must be a valid amount.")
    private BigDecimal price;

    @NotNull(message = "Stock is required.")
    @Min(value = 0, message = "Stock cannot be negative.")
    @Max(value = 1_000_000, message = "Stock is too large.")
    private Integer stock;
}
