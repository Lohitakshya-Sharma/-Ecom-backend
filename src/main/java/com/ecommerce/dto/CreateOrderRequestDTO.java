package com.ecommerce.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateOrderRequestDTO(
        @Schema(description = "Customer email address", example = "customer@example.com")
        @NotBlank(message = "Customer email is required")
        @Email(message = "Customer email must be a valid email format")
        String customerEmail,

        @Schema(description = "Customer full name", example = "John Doe")
        @NotBlank(message = "Customer name is required")
        String customerName,

        @Schema(description = "Customer delivery/shipping address", example = "123 Market St, San Francisco, CA")
        String shippingAddress,

        @NotEmpty(message = "Order must contain at least one item")
        @Valid
        List<OrderItemRequestDTO> items
) {
    public String getCustomerEmail() { return customerEmail(); }
    public String getCustomerName() { return customerName(); }
    public String getShippingAddress() { return shippingAddress(); }
    public List<OrderItemRequestDTO> getItems() { return items(); }
}
