package com.smartshop.customercredit.dto;

import com.smartshop.common.credit.CreditSource;
import com.smartshop.common.payment.CreditStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Ligne de la liste des credits clients (vente a credit ou credit saisi).
 *
 * @param id id de la vente (SALE) ou du credit saisi (MANUAL)
 */
public record CustomerCreditSummaryResponse(
        Long id,
        CreditSource source,
        String reference,
        String description,
        CustomerCreditResponse.Customer customer,
        Instant createdAt,
        String createdBy,
        BigDecimal total,
        BigDecimal amountPaid,
        BigDecimal amountDue,
        LocalDate dueDate,
        CreditStatus status
) {
}
