package com.ecommerce.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class ProductRequestDTO {

    @NotBlank
    private String name;

    @NotBlank
    private String description;

    @NotNull
    @Min(value = 1)
    private BigDecimal price;

    @NotNull
    private Boolean active;

    @NotNull
    @Min(0)
    private Integer stock;

    public String getName() { return name; }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() { return price; }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Integer getStock() { return stock; }

    public void setStock(Integer stock) { this.stock = stock; }
}
