package com.ecommerce.dto;

import com.ecommerce.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponseDTO(
        Long id,
        String customerEmail,
        String customerName,
        String shippingAddress,
        Instant orderDate,
        Instant updatedAt,
        OrderStatus status,
        BigDecimal totalAmount,
        List<OrderItemResponseDTO> orderItems
) {
    public Long getId() { return id(); }
    public String getCustomerEmail() { return customerEmail(); }
    public String getCustomerName() { return customerName(); }
    public String getShippingAddress() { return shippingAddress(); }
    public Instant getOrderDate() { return orderDate(); }
    public Instant getUpdatedAt() { return updatedAt(); }
    public OrderStatus getStatus() { return status(); }
    public BigDecimal getTotalAmount() { return totalAmount(); }
    public List<OrderItemResponseDTO> getOrderItems() { return orderItems(); }
}
