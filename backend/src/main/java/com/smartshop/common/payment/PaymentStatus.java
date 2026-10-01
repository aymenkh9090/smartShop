package com.smartshop.common.payment;

import java.math.BigDecimal;

public enum PaymentStatus {
    PAID,
    PARTIALLY_PAID,
    UNPAID;

    public static PaymentStatus of(BigDecimal total, BigDecimal amountPaid) {
        if (amountPaid.compareTo(total) >= 0) {
            return PAID;
        }
        return amountPaid.signum() > 0 ? PARTIALLY_PAID : UNPAID;
    }
}
