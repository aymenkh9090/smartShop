package com.smartshop.suppliercredit.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Solde du a un fournisseur : somme de ses credits en cours.
 */
public record SupplierBalanceResponse(
        Long supplierId,
        String name,
        String phone,
        long openCredits,
        BigDecimal outstanding,
        BigDecimal overdue,
        LocalDate nextDueDate
) {

    public SupplierBalanceResponse {
        overdue = overdue == null ? BigDecimal.ZERO : overdue;
    }
}
