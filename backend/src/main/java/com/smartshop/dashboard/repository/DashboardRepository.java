package com.smartshop.dashboard.repository;

import com.smartshop.aftersales.entity.AfterSalesStatus;
import com.smartshop.aftersales.entity.AfterSalesTicket;
import com.smartshop.common.payment.Money;
import com.smartshop.common.payment.PaymentStatus;
import com.smartshop.dashboard.dto.DashboardResponse.Credits;
import com.smartshop.dashboard.dto.DashboardResponse.LowStockProduct;
import com.smartshop.dashboard.dto.DashboardResponse.ReadyTicket;
import com.smartshop.dashboard.dto.DashboardResponse.TopProduct;
import com.smartshop.dashboard.dto.DashboardResponse.Totals;
import com.smartshop.sale.entity.SaleStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agregations en lecture seule pour le tableau de bord : uniquement des requetes count / sum / group by,
 * sans chargement d'entites (sauf la courte liste des tickets prets).
 * Les periodes sont des intervalles [from, to[ d'instants.
 */
@Repository
public class DashboardRepository {

    private static final List<AfterSalesStatus> CLOSED = List.of(AfterSalesStatus.DELIVERED, AfterSalesStatus.CANCELLED);

    @PersistenceContext
    private EntityManager em;

    /** Ventes non annulees de la periode : nombre et CA (total apres remise). */
    public Totals sales(Instant from, Instant to) {
        Object[] row = em.createQuery("""
                        select count(s), sum(s.total) from Sale s
                        where s.status = :completed and s.createdAt >= :from and s.createdAt < :to""", Object[].class)
                .setParameter("completed", SaleStatus.COMPLETED)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
        return totals(row);
    }

    public Totals purchases(Instant from, Instant to) {
        Object[] row = em.createQuery("""
                        select count(p), sum(p.total) from Purchase p
                        where p.createdAt >= :from and p.createdAt < :to""", Object[].class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
        return totals(row);
    }

    /** CA SAV : tickets livres dans la periode, montant facture (nul sous garantie). */
    public Totals deliveredAfterSales(Instant from, Instant to) {
        Object[] row = em.createQuery("""
                        select count(t), sum(t.totalCost) from AfterSalesTicket t
                        where t.status = :delivered and t.deliveredAt >= :from and t.deliveredAt < :to""", Object[].class)
                .setParameter("delivered", AfterSalesStatus.DELIVERED)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
        return totals(row);
    }

    /**
     * CA des ventes par jour (fuseau de la boutique), y compris les jours sans vente.
     *
     * @param firstDay premier jour inclus
     * @param lastDay  dernier jour inclus
     */
    public Map<LocalDate, BigDecimal> dailySales(LocalDate firstDay, LocalDate lastDay, ZoneId zone) {
        Map<LocalDate, BigDecimal> amounts = new LinkedHashMap<>();
        for (LocalDate day = firstDay; !day.isAfter(lastDay); day = day.plusDays(1)) {
            amounts.put(day, Money.ZERO);
        }
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                        select cast(created_at at time zone :zone as date) as day, sum(total)
                        from sale
                        where status = 'COMPLETED' and created_at >= :from and created_at < :to
                        group by day""")
                .setParameter("zone", zone.getId())
                .setParameter("from", firstDay.atStartOfDay(zone).toInstant())
                .setParameter("to", lastDay.plusDays(1).atStartOfDay(zone).toInstant())
                .getResultList();
        for (Object[] row : rows) {
            amounts.put(toLocalDate(row[0]), Money.of((BigDecimal) row[1]));
        }
        return amounts;
    }

    public List<TopProduct> topProducts(Instant from, Instant to, int limit) {
        return em.createQuery("""
                        select p.id, p.sku, p.name, sum(i.quantity), sum(i.lineTotal)
                        from SaleItem i join i.sale s join i.product p
                        where s.status = :completed and s.createdAt >= :from and s.createdAt < :to
                        group by p.id, p.sku, p.name
                        order by sum(i.quantity) desc, sum(i.lineTotal) desc, p.id""", Object[].class)
                .setParameter("completed", SaleStatus.COMPLETED)
                .setParameter("from", from)
                .setParameter("to", to)
                .setMaxResults(limit)
                .getResultStream()
                .map(row -> new TopProduct((Long) row[0], (String) row[1], (String) row[2],
                        ((Number) row[3]).longValue(), Money.of((BigDecimal) row[4])))
                .toList();
    }

    public long countLowStock() {
        return em.createQuery("select count(p) from Product p where p.active = true and p.quantity <= p.minStock",
                Long.class).getSingleResult();
    }

    /** Produits les plus critiques : plus grand deficit sous le seuil d'abord. */
    public List<LowStockProduct> lowStockProducts(int limit) {
        return em.createQuery("""
                        select p.id, p.sku, p.name, p.quantity, p.minStock from Product p
                        where p.active = true and p.quantity <= p.minStock
                        order by (p.quantity - p.minStock), p.quantity, p.name""", Object[].class)
                .setMaxResults(limit)
                .getResultStream()
                .map(row -> new LowStockProduct((Long) row[0], (String) row[1], (String) row[2],
                        (Integer) row[3], (Integer) row[4]))
                .toList();
    }

    /** Credits clients en cours : ventes a credit non soldees et non annulees. */
    public Credits customerCredits(LocalDate today) {
        Object[] row = em.createQuery("""
                        select count(s), sum(s.total - s.amountPaid),
                               sum(case when s.dueDate < :today then s.total - s.amountPaid else null end)
                        from Sale s
                        where s.onCredit = true and s.status = :completed and s.paymentStatus <> :paid""",
                        Object[].class)
                .setParameter("today", today)
                .setParameter("completed", SaleStatus.COMPLETED)
                .setParameter("paid", PaymentStatus.PAID)
                .getSingleResult();
        return credits(row);
    }

    public Credits supplierCredits(LocalDate today) {
        Object[] row = em.createQuery("""
                        select count(p), sum(p.total - p.amountPaid),
                               sum(case when p.dueDate < :today then p.total - p.amountPaid else null end)
                        from Purchase p
                        where p.onCredit = true and p.paymentStatus <> :paid""", Object[].class)
                .setParameter("today", today)
                .setParameter("paid", PaymentStatus.PAID)
                .getSingleResult();
        return credits(row);
    }

    /** Tickets en cours par statut ; tous les statuts ouverts sont presents (0 si aucun ticket). */
    public Map<AfterSalesStatus, Long> openTicketsByStatus() {
        Map<AfterSalesStatus, Long> counts = new EnumMap<>(AfterSalesStatus.class);
        for (AfterSalesStatus status : AfterSalesStatus.values()) {
            if (!status.isClosed()) {
                counts.put(status, 0L);
            }
        }
        em.createQuery("""
                        select t.status, count(t) from AfterSalesTicket t
                        where t.status not in :closed group by t.status""", Object[].class)
                .setParameter("closed", CLOSED)
                .getResultStream()
                .forEach(row -> counts.put((AfterSalesStatus) row[0], (Long) row[1]));
        return counts;
    }

    /** Tickets prets a livrer, les plus anciens d'abord. */
    public List<ReadyTicket> readyTickets(int limit) {
        return em.createQuery("""
                        select t from AfterSalesTicket t join fetch t.customer
                        where t.status = :ready order by t.receivedAt, t.id""", AfterSalesTicket.class)
                .setParameter("ready", AfterSalesStatus.READY)
                .setMaxResults(limit)
                .getResultStream()
                .map(t -> new ReadyTicket(t.getId(), t.getTicketNumber(),
                        t.getCustomer().getLastName() + " " + t.getCustomer().getFirstName(),
                        t.getCustomer().getPhone(), t.getDeviceDescription(), t.getAmountDue()))
                .toList();
    }

    private static Totals totals(Object[] row) {
        return new Totals(((Number) row[0]).longValue(), Money.of((BigDecimal) row[1]));
    }

    private static Credits credits(Object[] row) {
        return new Credits(((Number) row[0]).longValue(), Money.of((BigDecimal) row[1]), Money.of((BigDecimal) row[2]));
    }

    private static LocalDate toLocalDate(Object value) {
        return value instanceof Date date ? date.toLocalDate() : (LocalDate) value;
    }
}
