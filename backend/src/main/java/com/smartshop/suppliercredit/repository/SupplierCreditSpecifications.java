package com.smartshop.suppliercredit.repository;

import com.smartshop.common.payment.CreditStatus;
import com.smartshop.common.payment.PaymentStatus;
import com.smartshop.common.persistence.SearchPatterns;
import com.smartshop.purchase.entity.Purchase;
import com.smartshop.suppliercredit.dto.SupplierCreditFilter;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class SupplierCreditSpecifications {

    private SupplierCreditSpecifications() {
    }

    public static Specification<Purchase> matching(SupplierCreditFilter filter, LocalDate today) {
        List<Specification<Purchase>> specs = new ArrayList<>();
        specs.add((root, query, cb) -> cb.isTrue(root.get("onCredit")));
        if (StringUtils.hasText(filter.q())) {
            String pattern = SearchPatterns.containsPattern(filter.q());
            specs.add((root, query, cb) -> cb.or(
                    SearchPatterns.containsIgnoreCase(cb, root.get("reference"), pattern),
                    SearchPatterns.containsIgnoreCase(cb, root.get("supplierInvoiceRef"), pattern),
                    SearchPatterns.containsIgnoreCase(cb, root.get("supplier").get("name"), pattern)));
        }
        if (filter.supplierId() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("supplier").get("id"), filter.supplierId()));
        }
        if (filter.status() != null) {
            specs.add(hasStatus(filter.status(), today));
        }
        if (Boolean.TRUE.equals(filter.open())) {
            specs.add((root, query, cb) -> cb.notEqual(root.get("paymentStatus"), PaymentStatus.PAID));
        }
        return Specification.allOf(specs);
    }

    /** Traduction SQL de {@link CreditStatus#of} ; un achat n'est jamais annule (CANCELLED -> aucun resultat). */
    static Specification<Purchase> hasStatus(CreditStatus status, LocalDate today) {
        return (root, query, cb) -> {
            Path<PaymentStatus> paymentStatus = root.get("paymentStatus");
            Path<LocalDate> dueDate = root.get("dueDate");
            Predicate notOverdue = cb.or(cb.isNull(dueDate), cb.greaterThanOrEqualTo(dueDate, today));
            return switch (status) {
                case CANCELLED -> cb.disjunction();
                case PAID -> cb.equal(paymentStatus, PaymentStatus.PAID);
                case OVERDUE -> cb.and(cb.notEqual(paymentStatus, PaymentStatus.PAID), cb.lessThan(dueDate, today));
                case PARTIALLY_PAID -> cb.and(cb.equal(paymentStatus, PaymentStatus.PARTIALLY_PAID), notOverdue);
                case PENDING -> cb.and(cb.equal(paymentStatus, PaymentStatus.UNPAID), notOverdue);
            };
        };
    }
}
