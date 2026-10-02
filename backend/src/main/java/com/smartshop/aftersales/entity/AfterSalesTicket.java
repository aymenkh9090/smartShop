package com.smartshop.aftersales.entity;

import com.smartshop.common.entity.AuditableEntity;
import com.smartshop.common.exception.BusinessException;
import com.smartshop.common.exception.ResourceNotFoundException;
import com.smartshop.common.payment.Money;
import com.smartshop.common.payment.PaymentMethod;
import com.smartshop.common.payment.PaymentStatus;
import com.smartshop.customer.entity.Customer;
import com.smartshop.product.entity.Product;
import com.smartshop.sale.entity.Sale;
import com.smartshop.stock.entity.ProductUnit;
import com.smartshop.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Ticket SAV : agregat (historique des statuts, pieces, paiements) qui verifie lui-meme son workflow
 * et calcule ses montants. Un ticket livre est toujours solde.
 */
@Getter
@Entity
@Table(name = "after_sales_ticket")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AfterSalesTicket extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_number", nullable = false, unique = true, length = 30, updatable = false)
    private String ticketNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, updatable = false)
    private Customer customer;

    @Column(name = "device_description", nullable = false, length = 200, updatable = false)
    private String deviceDescription;

    /** Modele du catalogue, si l'appareil en fait partie. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", updatable = false)
    private Product product;

    /** Unite vendue par la boutique, retrouvee par son IMEI / numero de serie. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", updatable = false)
    private ProductUnit unit;

    /** Vente d'origine de l'unite (garantie). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", updatable = false)
    private Sale sale;

    @Column(length = 15, updatable = false)
    private String imei;

    @Column(name = "serial_number", length = 50, updatable = false)
    private String serialNumber;

    @Column(name = "reported_issue", nullable = false, length = 1000, updatable = false)
    private String reportedIssue;

    @Column(length = 2000)
    private String diagnosis;

    @Column(name = "repair_description", length = 2000)
    private String repairDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id")
    private User technician;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AfterSalesStatus status;

    /** Fin de garantie de la vente d'origine (null si l'appareil n'a pas ete vendu par la boutique). */
    @Column(name = "warranty_until", updatable = false)
    private LocalDate warrantyUntil;

    /** Reparation prise en charge par la garantie : rien n'est facture. */
    @Column(name = "under_warranty", nullable = false)
    private boolean underWarranty;

    /** Devis annonce au client (indicatif). */
    @Column(name = "estimated_cost", precision = 12, scale = 3)
    private BigDecimal estimatedCost;

    @Column(name = "labor_cost", nullable = false, precision = 12, scale = 3)
    private BigDecimal laborCost = Money.ZERO;

    @Column(name = "parts_total", nullable = false, precision = 12, scale = 3)
    private BigDecimal partsTotal = Money.ZERO;

    /** Montant facture : main-d'oeuvre + pieces, ou 0 sous garantie. */
    @Column(name = "total_cost", nullable = false, precision = 12, scale = 3)
    private BigDecimal totalCost = Money.ZERO;

    @Column(name = "amount_paid", nullable = false, precision = 12, scale = 3)
    private BigDecimal amountPaid = Money.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    @Column(name = "expected_date")
    private LocalDate expectedDate;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(length = 1000)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private User createdBy;

    @BatchSize(size = 50)
    @OrderBy("changedAt, id")
    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL)
    private List<AfterSalesStatusChange> statusHistory = new ArrayList<>();

    @BatchSize(size = 50)
    @OrderBy("id")
    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AfterSalesPart> parts = new ArrayList<>();

    @BatchSize(size = 50)
    @OrderBy("paidAt, id")
    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL)
    private List<AfterSalesPayment> payments = new ArrayList<>();

    /**
     * Appareil confie. Les references au catalogue, a l'unite et a la vente d'origine sont facultatives.
     */
    public record Device(String description, Product product, ProductUnit unit, Sale sale, String imei,
                         String serialNumber, LocalDate warrantyUntil) {
    }

    /**
     * @param receptionDate date (fuseau de la boutique) de reception : la garantie est appliquee si elle
     *                      couvre cette date
     */
    public AfterSalesTicket(String ticketNumber, Customer customer, Device device, String reportedIssue,
                            BigDecimal estimatedCost, LocalDate expectedDate, String notes,
                            LocalDate receptionDate, User createdBy, Instant receivedAt) {
        this.ticketNumber = ticketNumber;
        this.customer = customer;
        this.deviceDescription = device.description();
        this.product = device.product();
        this.unit = device.unit();
        this.sale = device.sale();
        this.imei = device.imei();
        this.serialNumber = device.serialNumber();
        this.warrantyUntil = device.warrantyUntil();
        this.underWarranty = isWarrantyValidOn(receptionDate);
        this.reportedIssue = reportedIssue;
        this.estimatedCost = estimatedCost == null ? null : Money.of(estimatedCost);
        this.expectedDate = expectedDate;
        this.notes = notes;
        this.createdBy = createdBy;
        this.receivedAt = receivedAt;
        this.status = AfterSalesStatus.RECEIVED;
        statusHistory.add(new AfterSalesStatusChange(this, null, status, null, createdBy, receivedAt));
        recalculate();
    }

    // ------------------------------------------------------------------ travail technique

    /**
     * Met a jour diagnostic, reparation et couts.
     *
     * @param receptionDate date de reception (fuseau de la boutique), pour verifier la garantie
     */
    public void updateWork(String diagnosis, String repairDescription, BigDecimal laborCost,
                           BigDecimal estimatedCost, LocalDate expectedDate, boolean underWarranty,
                           String notes, LocalDate receptionDate) {
        requireOpen();
        if (underWarranty && !isWarrantyValidOn(receptionDate)) {
            throw invalid(warrantyUntil == null
                    ? "Appareil non vendu par la boutique : aucune garantie a appliquer"
                    : "Garantie expiree le %s a la reception".formatted(warrantyUntil));
        }
        this.diagnosis = diagnosis;
        this.repairDescription = repairDescription;
        this.laborCost = Money.of(laborCost);
        this.estimatedCost = estimatedCost == null ? null : Money.of(estimatedCost);
        this.expectedDate = expectedDate;
        this.underWarranty = underWarranty;
        this.notes = notes;
        recalculate();
    }

    public void assignTechnician(User technician) {
        requireOpen();
        this.technician = technician;
    }

    /** Transition du workflow technique (voir {@link AfterSalesStatus#nextWorkflowStatuses()}). */
    public void changeStatus(AfterSalesStatus target, String comment, User user, Instant at) {
        requireOpen();
        if (!status.nextWorkflowStatuses().contains(target)) {
            throw new BusinessException("Transition impossible : %s -> %s".formatted(status, target));
        }
        if (status == AfterSalesStatus.DIAGNOSING && diagnosis == null) {
            throw new BusinessException("Renseignez le diagnostic avant de passer au statut " + target);
        }
        recordStatus(target, comment, user, at);
    }

    public AfterSalesPart addPart(Product partProduct, int quantity, List<ProductUnit> units, User user, Instant at) {
        requireInWorkshop();
        AfterSalesPart part = new AfterSalesPart(this, partProduct, quantity, units, user, at);
        parts.add(part);
        recalculate();
        return part;
    }

    /** Retire une piece (erreur, piece non utilisee) ; l'appelant la remet en stock. */
    public AfterSalesPart removePart(Long partId) {
        requireInWorkshop();
        AfterSalesPart part = parts.stream()
                .filter(p -> p.getId().equals(partId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Piece du ticket " + ticketNumber, partId));
        parts.remove(part);
        recalculate();
        return part;
    }

    public void requireInWorkshop() {
        if (!status.isInWorkshop()) {
            throw new BusinessException("Pieces modifiables uniquement pendant le diagnostic ou la reparation (statut %s)"
                    .formatted(status));
        }
    }

    // ------------------------------------------------------------------ paiement, livraison, annulation

    /** Acompte ou reglement. Le reste du est recalcule a partir des paiements, jamais modifie directement. */
    public AfterSalesPayment receivePayment(BigDecimal amount, PaymentMethod method, String reference, User user,
                                            Instant paidAt) {
        requireOpen();
        BigDecimal paid = Money.of(amount);
        if (paid.compareTo(getAmountDue()) > 0) {
            throw invalid("Le paiement (%s) depasse le reste du (%s)".formatted(paid, getAmountDue()));
        }
        AfterSalesPayment payment = new AfterSalesPayment(this, paid, method, reference, user, paidAt);
        payments.add(payment);
        amountPaid = amountPaid.add(paid);
        paymentStatus = PaymentStatus.of(totalCost, amountPaid);
        return payment;
    }

    /** Remise de l'appareil au client : le ticket doit etre pret et solde. */
    public void deliver(String comment, User user, Instant at) {
        if (status != AfterSalesStatus.READY) {
            throw new BusinessException("Seul un ticket pret peut etre livre (statut %s)".formatted(status));
        }
        if (getAmountDue().signum() > 0) {
            throw new BusinessException("Reste a payer avant livraison : %s".formatted(getAmountDue()));
        }
        deliveredAt = at;
        recordStatus(AfterSalesStatus.DELIVERED, comment, user, at);
    }

    /** Annule le ticket ; l'appelant remet les pieces en stock. Les paiements restent dans l'historique. */
    public void cancel(String reason, User user, Instant at) {
        requireOpen();
        recordStatus(AfterSalesStatus.CANCELLED, reason, user, at);
    }

    public BigDecimal getAmountDue() {
        return totalCost.subtract(amountPaid);
    }

    /** Statuts accessibles par le workflow technique depuis le statut courant (ordre du cycle de vie). */
    public List<AfterSalesStatus> getNextStatuses() {
        return status.nextWorkflowStatuses().stream().sorted(Comparator.naturalOrder()).toList();
    }

    // ------------------------------------------------------------------ regles internes

    private boolean isWarrantyValidOn(LocalDate date) {
        return warrantyUntil != null && !date.isAfter(warrantyUntil);
    }

    private void recordStatus(AfterSalesStatus target, String comment, User user, Instant at) {
        statusHistory.add(new AfterSalesStatusChange(this, status, target, comment, user, at));
        status = target;
    }

    private void recalculate() {
        partsTotal = Money.of(parts.stream().map(AfterSalesPart::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add));
        totalCost = underWarranty ? Money.ZERO : laborCost.add(partsTotal);
        if (amountPaid.compareTo(totalCost) > 0) {
            throw new BusinessException("Le montant deja paye (%s) depasse le nouveau cout (%s)"
                    .formatted(amountPaid, totalCost));
        }
        paymentStatus = PaymentStatus.of(totalCost, amountPaid);
    }

    private void requireOpen() {
        if (status.isClosed()) {
            throw new BusinessException("Le ticket %s est cloture (%s)".formatted(ticketNumber, status));
        }
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(message, HttpStatus.BAD_REQUEST);
    }
}
