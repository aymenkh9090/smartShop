package com.smartshop.customer.dto;

import java.time.Instant;

public record CustomerResponse(
        Long id,
        String firstName,
        String lastName,
        String phone,
        String email,
        String address,
        String cin,
        String notes,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
