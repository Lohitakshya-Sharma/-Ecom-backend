package com.ecommerce.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProductUpdateRequestDTO(
        @NotBlank(message = "Product name is required") String name,
        @NotBlank(message = "Product description is required") String description,
        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be at least 0.01")
        @Digits(integer = 10, fraction = 2) BigDecimal price,
        @NotNull(message = "Active flag is required") Boolean active
) {}