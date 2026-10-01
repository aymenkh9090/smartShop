package com.smartshop.stock.entity;

public enum ProductUnitStatus {
    IN_STOCK,
    SOLD,
    /** Sortie du stock hors vente (casse, perte, ajustement). */
    REMOVED
}
