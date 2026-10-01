package com.smartshop.common.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Montants en dinars tunisiens : 3 decimales (millimes), arrondi commercial.
 */
public final class Money {

    public static final int SCALE = 3;
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE, RoundingMode.UNNECESSARY);

    private Money() {
    }

    public static BigDecimal of(BigDecimal amount) {
        return amount == null ? ZERO : amount.setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal times(BigDecimal unitAmount, int quantity) {
        return of(unitAmount.multiply(BigDecimal.valueOf(quantity)));
    }
}
