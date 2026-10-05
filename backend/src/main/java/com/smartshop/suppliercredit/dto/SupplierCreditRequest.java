package com.smartshop.suppliercredit.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Credit fournisseur saisi a la main (sans achat enregistre).
 *
 * @param amount     montant du au fournisseur (reste a payer)
 * @param creditDate date du credit ; aujourd'hui par defaut
 * @param dueDate    echeance facultative, pas avant la date du credit
 */
public record SupplierCreditRequest(
        @NotNull Long supplierId,
        @NotNull @Positive @Digits(integer = 9, fraction = 3) BigDecimal amount,
        @PastOrPresent LocalDate creditDate,
        LocalDate dueDate,
        @Size(max = 255) String description
) {
}
