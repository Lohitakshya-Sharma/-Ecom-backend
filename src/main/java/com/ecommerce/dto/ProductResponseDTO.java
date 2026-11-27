package com.ecommerce.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponseDTO(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Boolean active,
        Integer stock,
        Instant createdAt,
        Instant updatedAt
) {
    public Long getId() { return id(); }
    public String getName() { return name(); }
    public String getDescription() { return description(); }
    public BigDecimal getPrice() { return price(); }
    public Boolean getActive() { return active(); }
    public Integer getStock() { return stock(); }
    public Instant getCreatedAt() { return createdAt(); }
    public Instant getUpdatedAt() { return updatedAt(); }
}
