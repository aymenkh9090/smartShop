package com.smartshop.aftersales.dto;

import com.smartshop.common.payment.PaymentRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Remise de l'appareil au client.
 *
 * @param payments reglement encaisse a la remise ; apres ces paiements, le ticket doit etre solde
 */
public record AfterSalesDeliverRequest(
        @Size(max = 10) @Valid List<@NotNull PaymentRequest> payments,
        @Size(max = 500) String comment
) {

    public List<PaymentRequest> paymentsOrEmpty() {
        return payments == null ? List.of() : payments;
    }
}
