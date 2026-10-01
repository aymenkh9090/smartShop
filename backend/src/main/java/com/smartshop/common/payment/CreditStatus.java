package com.smartshop.common.payment;

import java.time.LocalDate;

/**
 * Statut d'un credit (vente ou achat non solde a sa creation), calcule a partir des paiements et de l'echeance.
 */
public enum CreditStatus {
    PENDING,
    PARTIALLY_PAID,
    PAID,
    OVERDUE,
    CANCELLED;

    public static CreditStatus of(boolean cancelled, PaymentStatus paymentStatus, LocalDate dueDate, LocalDate today) {
        if (cancelled) {
            return CANCELLED;
        }
        if (paymentStatus == PaymentStatus.PAID) {
            return PAID;
        }
        if (dueDate != null && dueDate.isBefore(today)) {
            return OVERDUE;
        }
        return paymentStatus == PaymentStatus.PARTIALLY_PAID ? PARTIALLY_PAID : PENDING;
    }
}
