package com.smartshop.customercredit.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Solde d'un client : somme de ses credits en cours.
 *
 * @param outstanding reste du total
 * @param overdue     part du reste du dont l'echeance est depassee
 * @param nextDueDate plus proche echeance (null si aucune)
 */
public record CustomerBalanceResponse(
        Long customerId,
        String firstName,
        String lastName,
        String phone,
        long openCredits,
        BigDecimal outstanding,
        BigDecimal overdue,
        LocalDate nextDueDate
) {

    public CustomerBalanceResponse {
        overdue = overdue == null ? BigDecimal.ZERO : overdue;
    }
}
