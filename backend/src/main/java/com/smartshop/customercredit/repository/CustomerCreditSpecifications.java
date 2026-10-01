package com.smartshop.customercredit.repository;

import com.smartshop.common.payment.CreditStatus;
import com.smartshop.common.payment.PaymentStatus;
import com.smartshop.common.persistence.SearchPatterns;
import com.smartshop.customer.entity.Customer;
import com.smartshop.customercredit.dto.CustomerCreditFilter;
import com.smartshop.sale.entity.Sale;
import com.smartshop.sale.entity.SaleStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class CustomerCreditSpecifications {

    private CustomerCreditSpecifications() {
    }

    public static Specification<Sale> matching(CustomerCreditFilter filter, LocalDate today) {
        List<Specification<Sale>> specs = new ArrayList<>();
        specs.add((root, query, cb) -> cb.isTrue(root.get("onCredit")));
        if (StringUtils.hasText(filter.q())) {
            String pattern = SearchPatterns.containsPattern(filter.q());
            String phonePattern = SearchPatterns.containsPattern(filter.q().replaceAll("\\s+", ""));
            specs.add((root, query, cb) -> {
                Join<Sale, Customer> customer = root.join("customer", JoinType.LEFT);
                return cb.or(
                        SearchPatterns.containsIgnoreCase(cb, root.get("reference"), pattern),
                        SearchPatterns.containsIgnoreCase(cb, customer.get("lastName"), pattern),
                        SearchPatterns.containsIgnoreCase(cb, customer.get("firstName"), pattern),
                        SearchPatterns.containsIgnoreCase(cb, customer.get("phone"), phonePattern));
            });
        }
        if (filter.customerId() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("customer").get("id"), filter.customerId()));
        }
        if (filter.status() != null) {
            specs.add(hasStatus(filter.status(), today));
        }
        if (Boolean.TRUE.equals(filter.open())) {
            specs.add((root, query, cb) -> cb.and(
                    cb.equal(root.get("status"), SaleStatus.COMPLETED),
                    cb.notEqual(root.get("paymentStatus"), PaymentStatus.PAID)));
        }
        return Specification.allOf(specs);
    }

    /** Traduction SQL de {@link CreditStatus#of} : les deux doivent rester coherents (teste). */
    static Specification<Sale> hasStatus(CreditStatus status, LocalDate today) {
        return (root, query, cb) -> {
            Path<SaleStatus> saleStatus = root.get("status");
            Path<PaymentStatus> paymentStatus = root.get("paymentStatus");
            Path<LocalDate> dueDate = root.get("dueDate");
            Predicate active = cb.equal(saleStatus, SaleStatus.COMPLETED);
            Predicate notOverdue = cb.or(cb.isNull(dueDate), cb.greaterThanOrEqualTo(dueDate, today));
            return switch (status) {
                case CANCELLED -> cb.equal(saleStatus, SaleStatus.CANCELLED);
                case PAID -> cb.and(active, cb.equal(paymentStatus, PaymentStatus.PAID));
                case OVERDUE -> cb.and(active, cb.notEqual(paymentStatus, PaymentStatus.PAID),
                        cb.lessThan(dueDate, today));
                case PARTIALLY_PAID -> cb.and(active, cb.equal(paymentStatus, PaymentStatus.PARTIALLY_PAID), notOverdue);
                case PENDING -> cb.and(active, cb.equal(paymentStatus, PaymentStatus.UNPAID), notOverdue);
            };
        };
    }
}
