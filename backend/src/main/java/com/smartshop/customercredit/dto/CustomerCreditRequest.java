package com.smartshop.customercredit.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Credit client saisi a la main (sans vente).
 *
 * @param amount      montant du par le client (reste a payer)
 * @param creditDate  date du credit ; aujourd'hui par defaut
 * @param dueDate     echeance facultative, pas avant la date du credit
 * @param description objet du credit (ex. "iPhone 13 128 Go")
 */
public record CustomerCreditRequest(
        @NotNull Long customerId,
        @NotNull @Positive @Digits(integer = 9, fraction = 3) BigDecimal amount,
        @PastOrPresent LocalDate creditDate,
        LocalDate dueDate,
        @Size(max = 255) String description
) {
}
