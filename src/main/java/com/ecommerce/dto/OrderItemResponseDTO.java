package com.ecommerce.dto;

import java.math.BigDecimal;

public record OrderItemResponseDTO(
        Long id,
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
    public Long getId() { return id(); }
    public Long getProductId() { return productId(); }
    public String getProductName() { return productName(); }
    public Integer getQuantity() { return quantity(); }
    public BigDecimal getUnitPrice() { return unitPrice(); }
    public BigDecimal getSubtotal() { return subtotal(); }
}
