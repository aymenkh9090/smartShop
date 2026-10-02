package com.smartshop.aftersales.dto;

import com.smartshop.aftersales.entity.AfterSalesStatus;

import java.time.Instant;

/**
 * @param q    numero de ticket, IMEI, numero de serie, appareil, nom ou telephone du client
 * @param open true : tickets en cours (ni livres ni annules)
 * @param from reception, inclus (ISO-8601)
 * @param to   reception, exclu (ISO-8601)
 */
public record AfterSalesFilter(String q, AfterSalesStatus status, Long technicianId, Long customerId,
                               Boolean open, Instant from, Instant to) {
}
