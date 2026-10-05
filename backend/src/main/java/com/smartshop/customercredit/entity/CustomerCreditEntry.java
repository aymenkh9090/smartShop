package com.smartshop.customercredit.entity;

import com.smartshop.common.credit.CreditSource;
import com.smartshop.common.payment.PaymentStatus;
import com.smartshop.customer.entity.Customer;
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
 * Vue en lecture seule de tous les credits clients : ventes non soldees a leur creation et credits
 * saisis a la main. Sert aux listes, aux soldes et au tableau de bord.
 * Les references (V-..., CC-...) sont uniques sur l'ensemble.
 */
@Getter
@Entity
@Immutable
@Subselect("""
        select s.reference, 'SALE' as source, s.id as source_id, s.customer_id,
               cast(null as varchar(255)) as description, s.total, s.amount_paid, s.payment_status, s.due_date,
               s.status = 'CANCELLED' as cancelled, s.created_at, s.created_by
        from sale s
        where s.on_credit
        union all
        select c.reference, 'MANUAL', c.id, c.customer_id, c.description, c.amount, c.amount_paid,
               c.payment_status, c.due_date, c.cancelled_at is not null, c.credited_at, c.created_by
        from customer_credit c
        """)
@Synchronize({"sale", "customer_credit"})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomerCreditEntry {

    @Id
    private String reference;

    @Enumerated(EnumType.STRING)
    private CreditSource source;

    /** Id de la vente (SALE) ou du credit saisi (MANUAL). */
    @Column(name = "source_id")
    private Long sourceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

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

    /** Date du credit (date de la vente, ou date saisie du credit manuel). */
    @Column(name = "created_at")
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    public BigDecimal getAmountDue() {
        return total.subtract(amountPaid);
    }
}
