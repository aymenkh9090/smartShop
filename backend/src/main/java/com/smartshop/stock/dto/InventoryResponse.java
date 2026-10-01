package com.smartshop.stock.dto;

import java.util.List;

/**
 * @param countedProducts  nombre de produits comptes
 * @param adjustedProducts nombre de produits dont le stock a ete corrige
 * @param movements        ajustements crees
 */
public record InventoryResponse(int countedProducts, int adjustedProducts, List<StockMovementResponse> movements) {
}
