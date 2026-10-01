package com.smartshop.product.dto;

import com.smartshop.product.entity.ProductCondition;
import com.smartshop.product.entity.ProductType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Creation et modification d'un produit. La quantite n'en fait pas partie :
 * elle evolue uniquement par les mouvements de stock.
 */
public record ProductRequest(
        @NotBlank @Size(max = 50)
        @Pattern(regexp = "^\\s*[A-Za-z0-9._-]+\\s*$", message = "lettres, chiffres, '.', '_' ou '-' uniquement")
        String sku,

        @Size(max = 50)
        @Pattern(regexp = "^\\s*[A-Za-z0-9-]*\\s*$", message = "lettres, chiffres ou '-' uniquement")
        String barcode,

        @Size(max = 100) String reference,
        @NotBlank @Size(max = 150) String name,
        @Size(max = 2000) String description,
        @NotNull Long categoryId,
        @NotNull ProductType type,
        @NotNull ProductCondition condition,
        @Size(max = 100) String brand,
        @Size(max = 100) String model,

        @NotNull @PositiveOrZero @Digits(integer = 9, fraction = 3)
        BigDecimal purchasePrice,

        @NotNull @PositiveOrZero @Digits(integer = 9, fraction = 3)
        BigDecimal salePrice,

        @NotNull @PositiveOrZero @Max(1_000_000)
        Integer minStock,

        @NotNull Boolean serialTracked,

        @PositiveOrZero @Max(120)
        Integer warrantyMonths
) {
}
