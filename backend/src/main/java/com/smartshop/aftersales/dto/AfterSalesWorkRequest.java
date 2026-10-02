package com.smartshop.aftersales.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Travail du technicien : diagnostic, reparation et couts (remplace les valeurs courantes).
 *
 * @param underWarranty reparation prise en charge par la garantie (refuse si l'appareil n'est pas garanti)
 */
public record AfterSalesWorkRequest(
        @Size(max = 2000) String diagnosis,
        @Size(max = 2000) String repairDescription,
        @NotNull @PositiveOrZero @Digits(integer = 9, fraction = 3) BigDecimal laborCost,
        @PositiveOrZero @Digits(integer = 9, fraction = 3) BigDecimal estimatedCost,
        LocalDate expectedDate,
        @NotNull Boolean underWarranty,
        @Size(max = 1000) String notes
) {
}
