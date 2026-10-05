package com.smartshop.common.payment;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Versement sur un credit (tranche). Le reste du est recalcule par le systeme.
 *
 * @param method facultatif : especes par defaut
 * @param paidOn date du versement, pour la tracabilite ; aujourd'hui par defaut
 */
public record CreditPaymentRequest(
        @NotNull @Positive @Digits(integer = 9, fraction = 3) BigDecimal amount,
        PaymentMethod method,
        @Size(max = 100) String reference,
        @PastOrPresent LocalDate paidOn
) {

    public PaymentMethod methodOrDefault() {
        return method == null ? PaymentMethod.CASH : method;
    }
}
