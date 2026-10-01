package com.smartshop.sale.dto;

import com.smartshop.common.payment.PaymentRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Nouvelle vente. Les prix viennent du produit ; seuls quantites, remise et paiements sont saisis.
 *
 * @param customerId facultatif pour une vente comptant, obligatoire pour une vente a credit
 * @param payments   paiements recus a la vente ; leur somme ne peut pas depasser le total
 * @param dueDate    echeance du reste a payer (vente a credit)
 */
public record SaleRequest(
        Long customerId,
        @NotEmpty @Size(max = 100) @Valid List<@NotNull Item> items,
        @PositiveOrZero @Digits(integer = 9, fraction = 3) BigDecimal discount,
        @Size(max = 10) @Valid List<@NotNull PaymentRequest> payments,
        @FutureOrPresent LocalDate dueDate,
        @Size(max = 1000) String notes
) {

    public List<PaymentRequest> paymentsOrEmpty() {
        return payments == null ? List.of() : payments;
    }

    /**
     * @param unitIds unites vendues (IMEI) pour un produit suivi a l'unite : autant que la quantite
     */
    public record Item(
            @NotNull Long productId,
            @NotNull @Positive @Max(10_000) Integer quantity,
            @Size(max = 1000) List<@NotNull Long> unitIds
    ) {

        public List<Long> unitIdsOrEmpty() {
            return unitIds == null ? List.of() : unitIds;
        }
    }
}
