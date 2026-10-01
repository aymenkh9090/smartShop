package com.smartshop.purchase.dto;

import com.smartshop.common.payment.PaymentResponse;
import com.smartshop.common.payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PurchaseResponse(
        Long id,
        String reference,
        SupplierSummary supplier,
        String supplierInvoiceRef,
        List<Item> items,
        BigDecimal total,
        BigDecimal amountPaid,
        BigDecimal amountDue,
        PaymentStatus paymentStatus,
        LocalDate dueDate,
        List<PaymentResponse> payments,
        String notes,
        String createdBy,
        Instant createdAt
) {

    public record SupplierSummary(Long id, String name) {
    }

    public record Item(
            Long id,
            Long productId,
            String productSku,
            String productName,
            int quantity,
            BigDecimal unitCost,
            BigDecimal lineTotal,
            List<Unit> units
    ) {
    }

    public record Unit(Long id, String imei, String serialNumber) {
    }
}
