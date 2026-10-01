package com.smartshop.purchase.entity;

import com.smartshop.common.exception.BusinessException;
import com.smartshop.common.payment.Money;
import com.smartshop.common.payment.PaymentMethod;
import com.smartshop.common.payment.PaymentStatus;
import com.smartshop.product.entity.Product;
import com.smartshop.stock.entity.ProductUnit;
import com.smartshop.supplier.entity.Supplier;
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
 * Achat aupres d'un fournisseur (marchandise recue). Un reste a payer constitue un credit fournisseur.
 */
@Getter
@Entity
@Table(name = "purchase")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30, updatable = false)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false, updatable = false)
    private Supplier supplier;

    @Column(name = "supplier_invoice_ref", length = 100)
    private String supplierInvoiceRef;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal total = Money.ZERO;

    @Column(name = "amount_paid", nullable = false, precision = 12, scale = 3)
    private BigDecimal amountPaid = Money.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    @Column(name = "due_date")
    private LocalDate dueDate;

    /** Achat non solde a sa creation : credit fournisseur (historique) meme une fois regle. */
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

    @BatchSize(size = 50)
    @OrderBy("id")
    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseItem> items = new ArrayList<>();

    @BatchSize(size = 50)
    @OrderBy("paidAt, id")
    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL)
    private List<PurchasePayment> payments = new ArrayList<>();

    public Purchase(String reference, Supplier supplier, String supplierInvoiceRef, User createdBy, String notes) {
        this.reference = reference;
        this.supplier = supplier;
        this.supplierInvoiceRef = supplierInvoiceRef;
        this.createdBy = createdBy;
        this.notes = notes;
        this.paymentStatus = PaymentStatus.UNPAID;
    }

    public void addItem(Product product, int quantity, BigDecimal unitCost, List<ProductUnit> units) {
        items.add(new PurchaseItem(this, product, quantity, unitCost, units));
    }

    public void addPayment(BigDecimal amount, PaymentMethod method, String reference, User user, Instant paidAt) {
        payments.add(new PurchasePayment(this, amount, method, reference, user, paidAt));
    }

    /** Calcule les montants et verifie leur coherence ; l'echeance n'est conservee que s'il reste un du. */
    public void complete(LocalDate requestedDueDate) {
        if (items.isEmpty()) {
            throw new BusinessException("Un achat doit contenir au moins une ligne", HttpStatus.BAD_REQUEST);
        }
        total = Money.of(items.stream().map(PurchaseItem::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add));
        amountPaid = Money.of(payments.stream().map(PurchasePayment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        if (amountPaid.compareTo(total) > 0) {
            throw new BusinessException("Le montant paye (%s) depasse le total (%s)".formatted(amountPaid, total),
                    HttpStatus.BAD_REQUEST);
        }
        paymentStatus = PaymentStatus.of(total, amountPaid);
        dueDate = paymentStatus == PaymentStatus.PAID ? null : requestedDueDate;
        onCredit = paymentStatus != PaymentStatus.PAID;
    }

    /** Paiement verse au fournisseur apres l'achat ; le reste du est recalcule a partir des paiements. */
    public PurchasePayment makePayment(BigDecimal amount, PaymentMethod method, String reference, User user,
                                       Instant paidAt) {
        if (paymentStatus == PaymentStatus.PAID) {
            throw new BusinessException("L'achat %s est deja solde".formatted(this.reference));
        }
        BigDecimal paid = Money.of(amount);
        if (paid.compareTo(getAmountDue()) > 0) {
            throw new BusinessException("Le paiement (%s) depasse le reste du (%s)".formatted(paid, getAmountDue()),
                    HttpStatus.BAD_REQUEST);
        }
        PurchasePayment payment = new PurchasePayment(this, paid, method, reference, user, paidAt);
        payments.add(payment);
        amountPaid = amountPaid.add(paid);
        paymentStatus = PaymentStatus.of(total, amountPaid);
        return payment;
    }

    /** Reporte (ou supprime) l'echeance d'un credit fournisseur non solde. */
    public void reschedule(LocalDate newDueDate) {
        if (paymentStatus == PaymentStatus.PAID) {
            throw new BusinessException("Seul un credit en cours peut etre reporte");
        }
        dueDate = newDueDate;
    }

    public BigDecimal getAmountDue() {
        return total.subtract(amountPaid);
    }
}
