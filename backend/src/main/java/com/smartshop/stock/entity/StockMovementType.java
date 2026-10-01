package com.smartshop.stock.entity;

public enum StockMovementType {
    /** Entree (reception, achat) : quantite positive. */
    STOCK_IN,
    /** Sortie (perte, casse, usage interne, vente) : quantite positive, retiree du stock. */
    STOCK_OUT,
    /** Correction (inventaire) : variation signee. */
    ADJUSTMENT,
    /** Retour client : quantite positive, remise en stock. */
    RETURN
}
