package com.smartshop.common.payment;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Long id,
        BigDecimal amount,
        PaymentMethod method,
        String reference,
        Instant paidAt,
        String createdBy
) {
}
