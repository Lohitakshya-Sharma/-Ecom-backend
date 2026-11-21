package com.ecommerce.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderItemRequestDTO(
        @Schema(description = "ID of the product being purchased", example = "1")
        @NotNull(message = "Product ID is required")
        Long productId,

        @Schema(description = "Quantity of the product to order", example = "2")
        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be greater than zero")
        Integer quantity
) {
    public Long getProductId() { return productId(); }
    public Integer getQuantity() { return quantity(); }
}
