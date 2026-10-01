package com.smartshop.suppliercredit.dto;

import com.smartshop.common.payment.CreditStatus;
import com.smartshop.common.payment.PaymentResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Credit fournisseur : achat non solde a sa creation, avec l'historique de ses paiements.
 */
public record SupplierCreditResponse(
        Long purchaseId,
        String reference,
        String supplierInvoiceRef,
        Supplier supplier,
        Instant createdAt,
        String createdBy,
        BigDecimal total,
        BigDecimal amountPaid,
        BigDecimal amountDue,
        LocalDate dueDate,
        CreditStatus status,
        List<PaymentResponse> payments
) {

    public record Supplier(Long id, String name, String phone) {
    }
}
