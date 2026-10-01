package com.smartshop.common.payment;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Paiement saisi lors d'une vente ou d'un achat.
 */
public record PaymentRequest(
        @NotNull @Positive @Digits(integer = 9, fraction = 3) BigDecimal amount,
        @NotNull PaymentMethod method,
        @Size(max = 100) String reference
) {
}
