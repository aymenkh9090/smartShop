package com.smartshop.purchase.dto;

import com.smartshop.common.payment.PaymentStatus;

import java.time.Instant;

/**
 * @param q reference de l'achat, reference de facture fournisseur ou nom du fournisseur
 */
public record PurchaseFilter(String q, Long supplierId, PaymentStatus paymentStatus, Instant from, Instant to) {
}
