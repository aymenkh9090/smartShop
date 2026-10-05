package com.smartshop.customercredit.entity;

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
@Table(name = "customer_credit_payment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomerCreditPayment extends ManualCreditPayment {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "credit_id", nullable = false, updatable = false)
    private CustomerCredit credit;

    CustomerCreditPayment(CustomerCredit credit, BigDecimal amount, PaymentMethod method, String reference,
                          Instant paidAt, Instant recordedAt, User createdBy) {
        super(amount, method, reference, paidAt, recordedAt, createdBy);
        this.credit = credit;
    }
}
