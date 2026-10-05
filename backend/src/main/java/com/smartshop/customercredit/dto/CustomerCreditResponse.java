package com.smartshop.customercredit.dto;

import com.smartshop.common.credit.CreditSource;
import com.smartshop.common.payment.CreditStatus;
import com.smartshop.common.payment.PaymentResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Credit client avec l'historique complet de ses paiements : vente non soldee a sa creation (SALE)
 * ou credit saisi a la main (MANUAL).
 *
 * @param id        id de la vente (SALE) ou du credit saisi (MANUAL)
 * @param createdAt date du credit
 */
public record CustomerCreditResponse(
        Long id,
        CreditSource source,
        String reference,
        String description,
        Customer customer,
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

    public record Customer(Long id, String firstName, String lastName, String phone) {
    }
}
