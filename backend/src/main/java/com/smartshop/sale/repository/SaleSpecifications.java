package com.smartshop.sale.repository;

import com.smartshop.common.persistence.SearchPatterns;
import com.smartshop.customer.entity.Customer;
import com.smartshop.sale.dto.SaleFilter;
import com.smartshop.sale.entity.Sale;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class SaleSpecifications {

    private SaleSpecifications() {
    }

    public static Specification<Sale> matching(SaleFilter filter) {
        List<Specification<Sale>> specs = new ArrayList<>();
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
            specs.add((root, query, cb) -> cb.equal(root.get("status"), filter.status()));
        }
        if (filter.paymentStatus() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("paymentStatus"), filter.paymentStatus()));
        }
        if (filter.from() != null) {
            specs.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), filter.from()));
        }
        if (filter.to() != null) {
            specs.add((root, query, cb) -> cb.lessThan(root.get("createdAt"), filter.to()));
        }
        return Specification.allOf(specs);
    }
}
