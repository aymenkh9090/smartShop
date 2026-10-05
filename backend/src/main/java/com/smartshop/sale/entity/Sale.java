package com.smartshop.sale.entity;

import com.smartshop.common.exception.BusinessException;
import com.smartshop.common.payment.Money;
import com.smartshop.common.payment.PaymentMethod;
import com.smartshop.common.payment.PaymentStatus;
import com.smartshop.customer.entity.Customer;
import com.smartshop.product.entity.Product;
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
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Vente : agregat (lignes + paiements) qui calcule et verifie lui-meme ses montants.
 * Une vente non soldee est une vente a credit : elle exige un client.
 */
@Getter
@Entity
@Table(name = "sale")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30, updatable = false)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", updatable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SaleStatus status;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal subtotal = Money.ZERO;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal discount = Money.ZERO;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal total = Money.ZERO;

    @Column(name = "amount_paid", nullable = false, precision = 12, scale = 3)
    private BigDecimal amountPaid = Money.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    @Column(name = "due_date")
    private LocalDate dueDate;

    /** Vente non soldee a sa creation : reste un credit (historique) meme une fois reglee. */
    @Column(name = "on_credit", nullable = false, updatable = false)
    private boolean onCredit;

    @Column(length = 1000)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by")
    private User cancelledBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;

    @BatchSize(size = 50)
    @OrderBy("id")
    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleItem> items = new ArrayList<>();

    @BatchSize(size = 50)
    @OrderBy("paidAt, id")
    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL)
    private List<SalePayment> payments = new ArrayList<>();

    public Sale(String reference, Customer customer, User createdBy, String notes) {
        this.reference = reference;
        this.customer = customer;
        this.createdBy = createdBy;
        this.notes = notes;
        this.status = SaleStatus.COMPLETED;
        this.paymentStatus = PaymentStatus.UNPAID;
    }

    public void addItem(Product product, int quantity, List<ProductUnit> units) {
        items.add(new SaleItem(this, product, quantity, units));
    }

    public void addPayment(BigDecimal amount, PaymentMethod method, String reference, User user, Instant paidAt) {
        payments.add(new SalePayment(this, amount, method, reference, user, paidAt));
    }

    /**
     * Calcule les montants une fois lignes et paiements ajoutes, et verifie leur coherence.
     *
     * @param requestedDiscount remise globale (null = aucune)
     * @param requestedDueDate  echeance, conservee uniquement si un reste est du
     */
    public void complete(BigDecimal requestedDiscount, LocalDate requestedDueDate) {
        if (items.isEmpty()) {
            throw invalid("Une vente doit contenir au moins une ligne");
        }
        subtotal = Money.of(items.stream().map(SaleItem::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add));
        discount = Money.of(requestedDiscount);
        if (discount.compareTo(subtotal) > 0) {
            throw invalid("La remise (%s) dépasse le sous-total (%s)".formatted(discount, subtotal));
        }
        total = subtotal.subtract(discount);
        amountPaid = Money.of(payments.stream().map(SalePayment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        if (amountPaid.compareTo(total) > 0) {
            throw invalid("Le montant payé (%s) dépasse le total (%s)".formatted(amountPaid, total));
        }
        paymentStatus = PaymentStatus.of(total, amountPaid);
        if (paymentStatus != PaymentStatus.PAID && customer == null) {
            throw invalid("Une vente à crédit (reste à payer %s) nécessite un client".formatted(getAmountDue()));
        }
        dueDate = paymentStatus == PaymentStatus.PAID ? null : requestedDueDate;
        onCredit = paymentStatus != PaymentStatus.PAID;
    }

    /**
     * Paiement recu apres la vente (reglement d'un credit). Le reste du est recalcule a partir
     * de l'ensemble des paiements, jamais modifie directement.
     */
    public SalePayment receivePayment(BigDecimal amount, PaymentMethod method, String reference, User user,
                                      Instant paidAt) {
        if (isCancelled()) {
            throw new BusinessException("La vente %s est annulée : paiement impossible".formatted(this.reference));
        }
        if (paymentStatus == PaymentStatus.PAID) {
            throw new BusinessException("La vente %s est déjà soldée".formatted(this.reference));
        }
        BigDecimal paid = Money.of(amount);
        if (paid.compareTo(getAmountDue()) > 0) {
            throw invalid("Le paiement (%s) dépasse le reste dû (%s)".formatted(paid, getAmountDue()));
        }
        SalePayment payment = new SalePayment(this, paid, method, reference, user, paidAt);
        payments.add(payment);
        amountPaid = amountPaid.add(paid);
        paymentStatus = PaymentStatus.of(total, amountPaid);
        return payment;
    }

    /** Reporte (ou supprime) l'echeance d'un credit non solde. */
    public void reschedule(LocalDate newDueDate) {
        if (isCancelled() || paymentStatus == PaymentStatus.PAID) {
            throw new BusinessException("Seul un crédit en cours peut être reporté");
        }
        dueDate = newDueDate;
    }

    public BigDecimal getAmountDue() {
        return total.subtract(amountPaid);
    }

    public boolean isCancelled() {
        return status == SaleStatus.CANCELLED;
    }

    public void cancel(User user, Instant at, String reason) {
        if (isCancelled()) {
            throw new BusinessException("La vente %s est déjà annulée".formatted(reference));
        }
        status = SaleStatus.CANCELLED;
        cancelledBy = user;
        cancelledAt = at;
        cancelReason = reason;
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(message, HttpStatus.BAD_REQUEST);
    }
}
