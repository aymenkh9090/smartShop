package com.smartshop.aftersales.dto;

import com.smartshop.common.validation.Imei;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Reception d'un appareil. Si l'IMEI (ou le numero de serie) correspond a une unite vendue par la boutique,
 * le produit, la vente d'origine et la garantie sont retrouves automatiquement.
 *
 * @param productId     modele du catalogue (facultatif)
 * @param estimatedCost devis annonce au client (facultatif)
 * @param expectedDate  date de restitution prevue
 */
public record AfterSalesCreateRequest(
        @NotNull Long customerId,
        @NotBlank @Size(max = 200) String deviceDescription,
        Long productId,
        @Imei String imei,

        @Size(max = 50)
        @Pattern(regexp = "^\\s*[A-Za-z0-9/._-]*\\s*$", message = "lettres, chiffres, '/', '.', '_' ou '-' uniquement")
        String serialNumber,

        @NotBlank @Size(max = 1000) String reportedIssue,
        @PositiveOrZero @Digits(integer = 9, fraction = 3) BigDecimal estimatedCost,
        @FutureOrPresent LocalDate expectedDate,
        @Size(max = 1000) String notes
) {
}
