package com.smartshop.sale.dto;

import com.smartshop.common.payment.PaymentStatus;
import com.smartshop.sale.entity.SaleStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Ligne de la liste des ventes (sans lignes ni paiements). */
public record SaleSummaryResponse(
        Long id,
        String reference,
        SaleResponse.CustomerSummary customer,
        SaleStatus status,
        BigDecimal total,
        BigDecimal amountPaid,
        BigDecimal amountDue,
        PaymentStatus paymentStatus,
        LocalDate dueDate,
        String createdBy,
        Instant createdAt
) {
}
