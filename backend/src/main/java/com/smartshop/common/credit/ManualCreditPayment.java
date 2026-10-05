package com.smartshop.common.credit;

import com.smartshop.common.payment.Money;
import com.smartshop.common.payment.PaymentMethod;
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

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Versement immuable sur un credit saisi a la main.
 */
@Getter
@MappedSuperclass
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class ManualCreditPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false, precision = 12, scale = 3)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private PaymentMethod method;

    @Column(length = 100, updatable = false)
    private String reference;

    /** Date du versement declaree par l'utilisateur. */
    @Column(name = "paid_at", nullable = false, updatable = false)
    private Instant paidAt;

    /** Date de saisie dans le systeme. */
    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private User createdBy;

    protected ManualCreditPayment(BigDecimal amount, PaymentMethod method, String reference, Instant paidAt,
                                  Instant recordedAt, User createdBy) {
        this.amount = Money.of(amount);
        this.method = method;
        this.reference = reference;
        this.paidAt = paidAt;
        this.recordedAt = recordedAt;
        this.createdBy = createdBy;
    }
}
