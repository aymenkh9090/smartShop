package com.smartshop.suppliercredit.entity;

import com.smartshop.common.credit.CreditSource;
import com.smartshop.common.payment.PaymentStatus;
import com.smartshop.supplier.entity.Supplier;
import com.smartshop.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.Synchronize;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Vue en lecture seule de tous les credits fournisseurs : achats non soldes a leur creation et
 * credits saisis a la main. Les references (A-..., CF-...) sont uniques sur l'ensemble.
 */
@Getter
@Entity
@Immutable
@Subselect("""
        select p.reference, 'PURCHASE' as source, p.id as source_id, p.supplier_id,
               p.supplier_invoice_ref, cast(null as varchar(255)) as description, p.total, p.amount_paid,
               p.payment_status, p.due_date, false as cancelled, p.created_at, p.created_by
        from purchase p
        where p.on_credit
        union all
        select c.reference, 'MANUAL', c.id, c.supplier_id, null, c.description, c.amount, c.amount_paid,
               c.payment_status, c.due_date, c.cancelled_at is not null, c.credited_at, c.created_by
        from supplier_credit c
        """)
@Synchronize({"purchase", "supplier_credit"})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SupplierCreditEntry {

    @Id
    private String reference;

    @Enumerated(EnumType.STRING)
    private CreditSource source;

    /** Id de l'achat (PURCHASE) ou du credit saisi (MANUAL). */
    @Column(name = "source_id")
    private Long sourceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(name = "supplier_invoice_ref")
    private String supplierInvoiceRef;

    private String description;

    private BigDecimal total;

    @Column(name = "amount_paid")
    private BigDecimal amountPaid;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    private PaymentStatus paymentStatus;

    @Column(name = "due_date")
    private LocalDate dueDate;

    private boolean cancelled;

    /** Date du credit (date de l'achat, ou date saisie du credit manuel). */
    @Column(name = "created_at")
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    public BigDecimal getAmountDue() {
        return total.subtract(amountPaid);
    }
}
