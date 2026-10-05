package com.smartshop.suppliercredit.dto;

import com.smartshop.common.credit.CreditSource;
import com.smartshop.common.payment.CreditStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Ligne de la liste des credits fournisseurs (achat a credit ou credit saisi).
 *
 * @param id id de l'achat (PURCHASE) ou du credit saisi (MANUAL)
 */
public record SupplierCreditSummaryResponse(
        Long id,
        CreditSource source,
        String reference,
        String supplierInvoiceRef,
        String description,
        SupplierCreditResponse.Supplier supplier,
        Instant createdAt,
        String createdBy,
        BigDecimal total,
        BigDecimal amountPaid,
        BigDecimal amountDue,
        LocalDate dueDate,
        CreditStatus status
) {
}
