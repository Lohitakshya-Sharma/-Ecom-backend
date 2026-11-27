package com.ecommerce.dto;

import jakarta.validation.constraints.NotNull;

public record StockAdjustmentRequestDTO(
        @NotNull(message = "Stock delta is required") Integer delta
) {}