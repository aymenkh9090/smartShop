package com.smartshop.aftersales.dto;

import com.smartshop.aftersales.entity.AfterSalesStatus;
import com.smartshop.common.payment.PaymentResponse;
import com.smartshop.common.payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record AfterSalesResponse(
        Long id,
        String ticketNumber,
        CustomerSummary customer,
        String deviceDescription,
        ProductSummary product,
        Long unitId,
        Long saleId,
        String saleReference,
        String imei,
        String serialNumber,
        String reportedIssue,
        String diagnosis,
        String repairDescription,
        TechnicianResponse technician,
        AfterSalesStatus status,
        List<AfterSalesStatus> nextStatuses,
        LocalDate warrantyUntil,
        boolean underWarranty,
        BigDecimal estimatedCost,
        BigDecimal laborCost,
        BigDecimal partsTotal,
        BigDecimal totalCost,
        BigDecimal amountPaid,
        BigDecimal amountDue,
        PaymentStatus paymentStatus,
        Instant receivedAt,
        LocalDate expectedDate,
        Instant deliveredAt,
        String notes,
        String createdBy,
        List<StatusChange> statusHistory,
        List<Part> parts,
        List<PaymentResponse> payments
) {

    public record CustomerSummary(Long id, String firstName, String lastName, String phone) {
    }

    public record ProductSummary(Long id, String sku, String name) {
    }

    public record StatusChange(Long id, AfterSalesStatus fromStatus, AfterSalesStatus toStatus, String comment,
                               String changedBy, Instant changedAt) {
    }

    public record Part(Long id, Long productId, String productSku, String productName, int quantity,
                       BigDecimal unitPrice, BigDecimal lineTotal, List<Unit> units, String addedBy,
                       Instant addedAt) {
    }

    public record Unit(Long id, String imei, String serialNumber) {
    }
}
