package com.smartshop.suppliercredit.dto;

import com.smartshop.common.payment.CreditStatus;

/**
 * @param q      reference de l'achat, facture fournisseur ou nom du fournisseur
 * @param status statut calcule du credit
 * @param open   true : credits non soldes (PENDING, PARTIALLY_PAID, OVERDUE)
 */
public record SupplierCreditFilter(String q, Long supplierId, CreditStatus status, Boolean open) {
}
