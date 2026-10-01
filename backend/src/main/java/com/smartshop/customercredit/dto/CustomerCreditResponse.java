package com.smartshop.customercredit.dto;

import com.smartshop.common.payment.CreditStatus;
import com.smartshop.common.payment.PaymentResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Credit client : vente non soldee a sa creation, avec l'historique complet de ses paiements.
 */
public record CustomerCreditResponse(
        Long saleId,
        String reference,
        Customer customer,
        Instant createdAt,
        String createdBy,
        BigDecimal total,
        BigDecimal amountPaid,
        BigDecimal amountDue,
        LocalDate dueDate,
        CreditStatus status,
        List<PaymentResponse> payments
) {

    public record Customer(Long id, String firstName, String lastName, String phone) {
    }
}
