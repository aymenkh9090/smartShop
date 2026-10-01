package com.smartshop.customercredit.dto;

import com.smartshop.common.payment.CreditStatus;

/**
 * @param q      reference de la vente, nom ou telephone du client
 * @param status statut calcule du credit
 * @param open   true : credits non soldes et non annules (PENDING, PARTIALLY_PAID, OVERDUE)
 */
public record CustomerCreditFilter(String q, Long customerId, CreditStatus status, Boolean open) {
}
