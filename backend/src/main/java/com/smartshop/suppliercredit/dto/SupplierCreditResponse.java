package com.smartshop.suppliercredit.dto;

import com.smartshop.common.credit.CreditSource;
import com.smartshop.common.payment.CreditStatus;
import com.smartshop.common.payment.PaymentResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Credit fournisseur avec l'historique de ses paiements : achat non solde a sa creation (PURCHASE)
 * ou credit saisi a la main (MANUAL).
 *
 * @param id        id de l'achat (PURCHASE) ou du credit saisi (MANUAL)
 * @param createdAt date du credit
 */
public record SupplierCreditResponse(
        Long id,
        CreditSource source,
        String reference,
        String supplierInvoiceRef,
        String description,
        Supplier supplier,
        Instant createdAt,
        String createdBy,
        BigDecimal total,
        BigDecimal amountPaid,
        BigDecimal amountDue,
        LocalDate dueDate,
        CreditStatus status,
        String cancelReason,
        List<PaymentResponse> payments
) {

    public record Supplier(Long id, String name, String phone) {
    }
}
