package com.smartshop.aftersales.entity;

import com.smartshop.common.payment.Money;
import com.smartshop.common.payment.PaymentMethod;
import com.smartshop.user.entity.User;
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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/** Paiement recu pour une reparation : immuable. */
@Getter
@Entity
@Table(name = "after_sales_payment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AfterSalesPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false, updatable = false)
    private AfterSalesTicket ticket;

    @Column(nullable = false, updatable = false, precision = 12, scale = 3)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private PaymentMethod method;

    @Column(length = 100, updatable = false)
    private String reference;

    @Column(name = "paid_at", nullable = false, updatable = false)
    private Instant paidAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private User createdBy;

    AfterSalesPayment(AfterSalesTicket ticket, BigDecimal amount, PaymentMethod method, String reference,
                      User createdBy, Instant paidAt) {
        this.ticket = ticket;
        this.amount = Money.of(amount);
        this.method = method;
        this.reference = reference;
        this.createdBy = createdBy;
        this.paidAt = paidAt;
    }
}
