package com.smartshop.product.dto;

import com.smartshop.product.entity.ProductCondition;
import com.smartshop.product.entity.ProductType;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
        Long id,
        String sku,
        String barcode,
        String reference,
        String name,
        String description,
        CategorySummary category,
        ProductType type,
        ProductCondition condition,
        String brand,
        String model,
        BigDecimal purchasePrice,
        BigDecimal salePrice,
        int quantity,
        int minStock,
        boolean lowStock,
        boolean serialTracked,
        Integer warrantyMonths,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {

    public record CategorySummary(Long id, String name) {
    }
}
