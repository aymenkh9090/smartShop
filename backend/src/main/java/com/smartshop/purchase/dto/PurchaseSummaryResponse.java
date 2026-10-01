package com.smartshop.purchase.dto;

import com.smartshop.common.payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record PurchaseSummaryResponse(
        Long id,
        String reference,
        PurchaseResponse.SupplierSummary supplier,
        String supplierInvoiceRef,
        BigDecimal total,
        BigDecimal amountPaid,
        BigDecimal amountDue,
        PaymentStatus paymentStatus,
        LocalDate dueDate,
        String createdBy,
        Instant createdAt
) {
}
