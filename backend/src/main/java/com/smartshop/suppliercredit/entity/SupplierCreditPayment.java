package com.smartshop.suppliercredit.entity;

import com.smartshop.common.credit.ManualCreditPayment;
import com.smartshop.common.payment.PaymentMethod;
import com.smartshop.user.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Entity
@Table(name = "supplier_credit_payment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SupplierCreditPayment extends ManualCreditPayment {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "credit_id", nullable = false, updatable = false)
    private SupplierCredit credit;

    SupplierCreditPayment(SupplierCredit credit, BigDecimal amount, PaymentMethod method, String reference,
                          Instant paidAt, Instant recordedAt, User createdBy) {
        super(amount, method, reference, paidAt, recordedAt, createdBy);
        this.credit = credit;
    }
}
