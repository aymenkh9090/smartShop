package com.smartshop.supplier.dto;

import java.time.Instant;

public record SupplierResponse(
        Long id,
        String name,
        String phone,
        String email,
        String address,
        String taxId,
        String notes,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
