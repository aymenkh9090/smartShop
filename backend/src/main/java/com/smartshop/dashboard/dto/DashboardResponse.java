package com.smartshop.dashboard.dto;

import com.smartshop.aftersales.entity.AfterSalesStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Indicateurs du tableau de bord. Une section vaut null si le role de l'utilisateur n'y a pas acces :
 * elle n'est alors ni calculee ni exposee.
 *
 * @param date               jour de reference (fuseau de la boutique)
 * @param todaySales         ventes du jour (SELLER)
 * @param monthSales         ventes du mois en cours, comparees a la meme periode du mois precedent (MANAGER)
 * @param monthPurchases     achats du mois en cours (MANAGER)
 * @param lowStock           produits actifs sous le seuil minimum (tous les roles)
 * @param customerCredits    encours des credits clients (MANAGER)
 * @param supplierCredits    encours des credits fournisseurs (MANAGER)
 * @param afterSales         tickets SAV en cours et prets (SELLER, TECHNICIAN)
 * @param monthAfterSales    CA SAV du mois : tickets livres, montant facture (MANAGER)
 * @param topProducts        produits les plus vendus du mois (MANAGER)
 * @param dailyRevenue       CA des ventes par jour sur les 30 derniers jours, jours sans vente inclus (MANAGER)
 */
public record DashboardResponse(
        LocalDate date,
        Totals todaySales,
        MonthTotals monthSales,
        Totals monthPurchases,
        LowStock lowStock,
        Credits customerCredits,
        Credits supplierCredits,
        AfterSales afterSales,
        Totals monthAfterSales,
        List<TopProduct> topProducts,
        List<DailyAmount> dailyRevenue
) {

    public record Totals(long count, BigDecimal amount) {
    }

    /** @param previousAmount meme periode du mois precedent (du 1er au meme jour) */
    public record MonthTotals(long count, BigDecimal amount, BigDecimal previousAmount) {
    }

    public record LowStock(long count, List<LowStockProduct> products) {
    }

    public record LowStockProduct(Long id, String sku, String name, int quantity, int minStock) {
    }

    /** @param overdue part de l'encours dont l'echeance est depassee */
    public record Credits(long count, BigDecimal outstanding, BigDecimal overdue) {
    }

    /** @param byStatus nombre de tickets en cours par statut (statuts ouverts, zeros inclus) */
    public record AfterSales(long openCount, Map<AfterSalesStatus, Long> byStatus, List<ReadyTicket> ready) {
    }

    public record ReadyTicket(Long id, String ticketNumber, String customerName, String customerPhone,
                              String deviceDescription, BigDecimal amountDue) {
    }

    public record TopProduct(Long productId, String sku, String name, long quantity, BigDecimal revenue) {
    }

    public record DailyAmount(LocalDate date, BigDecimal amount) {
    }
}
