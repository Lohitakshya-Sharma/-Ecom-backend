package com.ecommerce.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponseDTO(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Boolean active,
        Integer stock,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public Long getId() { return id(); }
    public String getName() { return name(); }
    public String getDescription() { return description(); }
    public BigDecimal getPrice() { return price(); }
    public Boolean getActive() { return active(); }
    public Integer getStock() { return stock(); }
    public LocalDateTime getCreatedAt() { return createdAt(); }
    public LocalDateTime getUpdatedAt() { return updatedAt(); }
}
