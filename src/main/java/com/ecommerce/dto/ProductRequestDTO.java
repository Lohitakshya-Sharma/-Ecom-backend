package com.ecommerce.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductRequestDTO(
        @Schema(description = "Product display name", example = "iPhone 17")
        @NotBlank(message = "Product name is required")
        String name,

        @Schema(description = "Detailed product description", example = "Apple smartphone with A19 chip")
        @NotBlank(message = "Product description is required")
        String description,

        @Schema(description = "Product price in currency units (supports decimals)", example = "79999.00")
        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be at least 0.01")
        @Digits(integer = 10, fraction = 2, message = "Price cannot exceed 10 integer digits and 2 decimal places")
        BigDecimal price,

        @Schema(description = "Whether the product is currently active for purchase", example = "true")
        @NotNull(message = "Active flag is required")
        Boolean active,

        @Schema(description = "Initial stock quantity in inventory", example = "50")
        @NotNull(message = "Stock is required")
        @Min(value = 0, message = "Stock must be zero or greater")
        Integer stock
) {
    public String getName() { return name(); }
    public String getDescription() { return description(); }
    public BigDecimal getPrice() { return price(); }
    public Boolean getActive() { return active(); }
    public Integer getStock() { return stock(); }
}
