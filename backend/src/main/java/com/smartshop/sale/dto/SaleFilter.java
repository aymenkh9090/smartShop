package com.smartshop.sale.dto;

import com.smartshop.common.payment.PaymentStatus;
import com.smartshop.sale.entity.SaleStatus;

import java.time.Instant;

/**
 * @param q    reference de la vente, nom ou telephone du client
 * @param from inclus (ISO-8601)
 * @param to   exclu (ISO-8601)
 */
public record SaleFilter(String q, Long customerId, SaleStatus status, PaymentStatus paymentStatus,
                         Instant from, Instant to) {
}
