package com.smartshop.sale.dto;

import com.smartshop.common.payment.PaymentResponse;
import com.smartshop.common.payment.PaymentStatus;
import com.smartshop.sale.entity.SaleStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record SaleResponse(
        Long id,
        String reference,
        CustomerSummary customer,
        SaleStatus status,
        List<Item> items,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total,
        BigDecimal amountPaid,
        BigDecimal amountDue,
        PaymentStatus paymentStatus,
        LocalDate dueDate,
        List<PaymentResponse> payments,
        String notes,
        String createdBy,
        Instant createdAt,
        String cancelledBy,
        Instant cancelledAt,
        String cancelReason
) {

    public record CustomerSummary(Long id, String firstName, String lastName, String phone) {
    }

    public record Item(
            Long id,
            Long productId,
            String productSku,
            String productName,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal,
            Integer warrantyMonths,
            List<Unit> units
    ) {
    }

    public record Unit(Long id, String imei, String serialNumber) {
    }
}
