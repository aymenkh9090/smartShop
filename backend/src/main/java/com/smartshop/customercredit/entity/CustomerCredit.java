package com.smartshop.customercredit.entity;

import com.smartshop.common.credit.ManualCredit;
import com.smartshop.common.payment.PaymentMethod;
import com.smartshop.customer.entity.Customer;
import com.smartshop.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Credit client saisi a la main : le client doit un montant, il le rembourse par tranches.
 */
@Getter
@Entity
@Table(name = "customer_credit")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomerCredit extends ManualCredit<CustomerCreditPayment> {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, updatable = false)
    private Customer customer;

    @BatchSize(size = 50)
    @OrderBy("paidAt, id")
    @OneToMany(mappedBy = "credit", cascade = CascadeType.ALL)
    private List<CustomerCreditPayment> payments = new ArrayList<>();

    public CustomerCredit(String reference, Customer customer, BigDecimal amount, Instant creditedAt,
                          LocalDate dueDate, String description, User createdBy) {
        super(reference, amount, creditedAt, dueDate, description, createdBy);
        this.customer = customer;
    }

    @Override
    protected CustomerCreditPayment newPayment(BigDecimal amount, PaymentMethod method, String reference,
                                               Instant paidAt, Instant recordedAt, User user) {
        return new CustomerCreditPayment(this, amount, method, reference, paidAt, recordedAt, user);
    }
}
