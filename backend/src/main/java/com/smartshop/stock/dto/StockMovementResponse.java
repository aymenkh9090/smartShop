package com.smartshop.stock.dto;

import com.smartshop.stock.entity.StockMovementType;

import java.time.Instant;
import java.util.List;

public record StockMovementResponse(
        Long id,
        ProductSummary product,
        StockMovementType type,
        int quantityChange,
        int quantityAfter,
        String reason,
        String reference,
        UserSummary createdBy,
        Instant createdAt,
        List<UnitSummary> units
) {

    public record ProductSummary(Long id, String sku, String name) {
    }

    public record UserSummary(Long id, String username) {
    }

    public record UnitSummary(Long id, String imei, String serialNumber) {
    }
}
