package com.smartshop.aftersales.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Piece utilisee, sortie du stock.
 *
 * @param unitIds unites (IMEI / numero de serie) pour une piece suivie a l'unite : autant que la quantite
 */
public record AfterSalesPartRequest(
        @NotNull Long productId,
        @NotNull @Positive @Max(100) Integer quantity,
        @Size(max = 100) List<@NotNull Long> unitIds
) {

    public List<Long> unitIdsOrEmpty() {
        return unitIds == null ? List.of() : unitIds;
    }
}
