package com.smartshop.stock.dto;

import com.smartshop.common.validation.Imei;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Unite entrant en stock : au moins un identifiant (IMEI ou numero de serie).
 */
public record ProductUnitRequest(
        @Imei String imei,

        @Size(max = 50)
        @Pattern(regexp = "^\\s*[A-Za-z0-9/._-]*\\s*$", message = "lettres, chiffres, '/', '.', '_' ou '-' uniquement")
        String serialNumber
) {
}
