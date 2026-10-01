package com.smartshop.stock.dto;

import com.smartshop.stock.entity.ProductUnitStatus;

import java.time.Instant;

public record ProductUnitResponse(
        Long id,
        Long productId,
        String productSku,
        String productName,
        String imei,
        String serialNumber,
        ProductUnitStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
