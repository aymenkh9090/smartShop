package com.smartshop.aftersales.dto;

import com.smartshop.aftersales.entity.AfterSalesStatus;
import com.smartshop.common.payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Ligne de la liste des tickets (sans historique, pieces ni paiements). */
public record AfterSalesSummaryResponse(
        Long id,
        String ticketNumber,
        AfterSalesResponse.CustomerSummary customer,
        String deviceDescription,
        String imei,
        String serialNumber,
        TechnicianResponse technician,
        AfterSalesStatus status,
        boolean underWarranty,
        BigDecimal totalCost,
        BigDecimal amountDue,
        PaymentStatus paymentStatus,
        Instant receivedAt,
        LocalDate expectedDate
) {
}
