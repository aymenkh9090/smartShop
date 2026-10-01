package com.smartshop.stock.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Resultat d'un comptage physique : un ajustement est cree pour chaque ecart.
 */
public record InventoryRequest(
        @Size(max = 100) String reference,
        @Size(max = 255) String reason,
        @NotEmpty @Size(max = 1000) @Valid List<@NotNull Line> lines
) {

    public record Line(
            @NotNull Long productId,
            @NotNull @PositiveOrZero @Max(1_000_000) Integer countedQuantity
    ) {
    }
}
