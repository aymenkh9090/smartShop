package com.smartshop.suppliercredit.entity;

import com.smartshop.common.credit.ManualCredit;
import com.smartshop.common.payment.PaymentMethod;
import com.smartshop.supplier.entity.Supplier;
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
 * Credit fournisseur saisi a la main : la boutique doit un montant au fournisseur et le regle par tranches.
 */
@Getter
@Entity
@Table(name = "supplier_credit")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SupplierCredit extends ManualCredit<SupplierCreditPayment> {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false, updatable = false)
    private Supplier supplier;

    @BatchSize(size = 50)
    @OrderBy("paidAt, id")
    @OneToMany(mappedBy = "credit", cascade = CascadeType.ALL)
    private List<SupplierCreditPayment> payments = new ArrayList<>();

    public SupplierCredit(String reference, Supplier supplier, BigDecimal amount, Instant creditedAt,
                          LocalDate dueDate, String description, User createdBy) {
        super(reference, amount, creditedAt, dueDate, description, createdBy);
        this.supplier = supplier;
    }

    @Override
    protected SupplierCreditPayment newPayment(BigDecimal amount, PaymentMethod method, String reference,
                                               Instant paidAt, Instant recordedAt, User user) {
        return new SupplierCreditPayment(this, amount, method, reference, paidAt, recordedAt, user);
    }
}
