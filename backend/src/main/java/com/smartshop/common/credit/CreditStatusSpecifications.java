package com.smartshop.common.credit;

import com.smartshop.common.payment.CreditStatus;
import com.smartshop.common.payment.PaymentStatus;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

/**
 * Filtres SQL des vues "credits" (attributs {@code cancelled}, {@code paymentStatus}, {@code dueDate}).
 */
public final class CreditStatusSpecifications {

    private CreditStatusSpecifications() {
    }

    /** Traduction SQL de {@link CreditStatus#of} : les deux doivent rester coherents (teste). */
    public static <T> Specification<T> hasStatus(CreditStatus status, LocalDate today) {
        return (root, query, cb) -> {
            Path<Boolean> cancelled = root.get("cancelled");
            Path<PaymentStatus> paymentStatus = root.get("paymentStatus");
            Path<LocalDate> dueDate = root.get("dueDate");
            Predicate active = cb.isFalse(cancelled);
            Predicate notOverdue = cb.or(cb.isNull(dueDate), cb.greaterThanOrEqualTo(dueDate, today));
            return switch (status) {
                case CANCELLED -> cb.isTrue(cancelled);
                case PAID -> cb.and(active, cb.equal(paymentStatus, PaymentStatus.PAID));
                case OVERDUE -> cb.and(active, cb.notEqual(paymentStatus, PaymentStatus.PAID),
                        cb.lessThan(dueDate, today));
                case PARTIALLY_PAID -> cb.and(active, cb.equal(paymentStatus, PaymentStatus.PARTIALLY_PAID), notOverdue);
                case PENDING -> cb.and(active, cb.equal(paymentStatus, PaymentStatus.UNPAID), notOverdue);
            };
        };
    }

    /** Credits en cours : non soldes et non annules. */
    public static <T> Specification<T> open() {
        return (root, query, cb) -> cb.and(cb.isFalse(root.get("cancelled")),
                cb.notEqual(root.get("paymentStatus"), PaymentStatus.PAID));
    }
}
