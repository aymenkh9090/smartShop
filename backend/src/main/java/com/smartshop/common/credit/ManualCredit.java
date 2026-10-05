package com.smartshop.common.credit;

import com.smartshop.common.exception.BusinessException;
import com.smartshop.common.payment.Money;
import com.smartshop.common.payment.PaymentMethod;
import com.smartshop.common.payment.PaymentStatus;
import com.smartshop.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Credit saisi a la main (sans vente ni achat) : un montant du, puis des versements successifs.
 * Le montant paye et le statut sont recalcules a chaque versement, jamais saisis.
 *
 * @param <P> type des versements
 */
@Getter
@MappedSuperclass
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class ManualCredit<P extends ManualCreditPayment> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30, updatable = false)
    private String reference;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, updatable = false, precision = 12, scale = 3)
    private BigDecimal amount;

    @Column(name = "amount_paid", nullable = false, precision = 12, scale = 3)
    private BigDecimal amountPaid = Money.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    /** Date a laquelle la dette est nee. */
    @Column(name = "credited_at", nullable = false, updatable = false)
    private Instant creditedAt;

    @Column(name = "due_date")
    private LocalDate dueDate;

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

    protected ManualCredit(String reference, BigDecimal amount, Instant creditedAt, LocalDate dueDate,
                           String description, User createdBy) {
        if (amount == null || Money.of(amount).signum() <= 0) {
            throw invalid("Le montant du crédit doit être strictement positif");
        }
        this.reference = reference;
        this.amount = Money.of(amount);
        this.creditedAt = creditedAt;
        this.dueDate = dueDate;
        this.description = description;
        this.createdBy = createdBy;
    }

    /** Versements, du plus ancien au plus recent. */
    public abstract List<P> getPayments();

    protected abstract P newPayment(BigDecimal amount, PaymentMethod method, String reference, Instant paidAt,
                                    Instant recordedAt, User user);

    /** Enregistre une tranche ; le reste du est recalcule. */
    public P pay(BigDecimal amount, PaymentMethod method, String reference, Instant paidAt, Instant recordedAt,
                 User user) {
        if (isCancelled()) {
            throw new BusinessException("Le crédit %s est annulé : paiement impossible".formatted(this.reference));
        }
        if (paymentStatus == PaymentStatus.PAID) {
            throw new BusinessException("Le crédit %s est déjà soldé".formatted(this.reference));
        }
        BigDecimal paid = Money.of(amount);
        if (paid.compareTo(getAmountDue()) > 0) {
            throw invalid("Le paiement (%s) dépasse le reste dû (%s)".formatted(paid, getAmountDue()));
        }
        if (paidAt.isBefore(creditedAt)) {
            throw invalid("La date du paiement précède la date du crédit");
        }
        P payment = newPayment(paid, method, reference, paidAt, recordedAt, user);
        getPayments().add(payment);
        amountPaid = amountPaid.add(paid);
        paymentStatus = PaymentStatus.of(this.amount, amountPaid);
        return payment;
    }

    /** Reporte (ou supprime) l'echeance d'un credit non solde. */
    public void reschedule(LocalDate newDueDate) {
        if (isCancelled() || paymentStatus == PaymentStatus.PAID) {
            throw new BusinessException("Seul un crédit en cours peut être reporté");
        }
        dueDate = newDueDate;
    }

    /** Annule un credit saisi par erreur ; impossible des qu'un versement a ete enregistre. */
    public void cancel(User user, Instant at, String reason) {
        if (isCancelled()) {
            throw new BusinessException("Le crédit %s est déjà annulé".formatted(reference));
        }
        if (amountPaid.signum() > 0) {
            throw new BusinessException("Le crédit %s a déjà des paiements : annulation impossible".formatted(reference));
        }
        cancelledBy = user;
        cancelledAt = at;
        cancelReason = reason;
    }

    public BigDecimal getAmountDue() {
        return amount.subtract(amountPaid);
    }

    public boolean isCancelled() {
        return cancelledAt != null;
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(message, HttpStatus.BAD_REQUEST);
    }
}
