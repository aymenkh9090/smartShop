package com.smartshop.stock.dto;

import com.smartshop.stock.entity.ProductUnitStatus;

/**
 * @param q recherche dans l'IMEI et le numero de serie
 */
public record ProductUnitFilter(Long productId, ProductUnitStatus status, String q) {
}
