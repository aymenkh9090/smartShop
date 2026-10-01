package com.smartshop.stock.dto;

import com.smartshop.stock.entity.StockMovementType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Mouvement de stock manuel.
 *
 * @param quantity positive pour STOCK_IN, STOCK_OUT et RETURN ; signee (non nulle) pour ADJUSTMENT
 * @param units    unites entrantes d'un produit suivi a l'unite (STOCK_IN, ADJUSTMENT positif)
 * @param unitIds  unites existantes concernees d'un produit suivi (STOCK_OUT, ADJUSTMENT negatif, RETURN)
 */
public record StockMovementRequest(
        @NotNull Long productId,
        @NotNull StockMovementType type,
        @NotNull @Min(-100_000) @Max(100_000) Integer quantity,
        @Size(max = 255) String reason,
        @Size(max = 100) String reference,
        @Valid @Size(max = 1000) List<@NotNull ProductUnitRequest> units,
        @Size(max = 1000) List<@NotNull Long> unitIds
) {

    public List<ProductUnitRequest> unitsOrEmpty() {
        return units == null ? List.of() : units;
    }

    public List<Long> unitIdsOrEmpty() {
        return unitIds == null ? List.of() : unitIds;
    }
}
