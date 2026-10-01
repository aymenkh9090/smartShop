package com.smartshop.stock.dto;

import com.smartshop.stock.entity.StockMovementType;

import java.time.Instant;

/**
 * @param from inclus (ISO-8601, ex. 2026-10-01T00:00:00Z)
 * @param to   exclu
 * @param q    texte recherche dans le nom / SKU du produit, la reference et le motif
 */
public record StockMovementFilter(Long productId, StockMovementType type, Instant from, Instant to, String q) {
}
