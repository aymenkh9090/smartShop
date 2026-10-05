package com.smartshop.customercredit.repository;

import com.smartshop.common.credit.CreditStatusSpecifications;
import com.smartshop.common.persistence.SearchPatterns;
import com.smartshop.customer.entity.Customer;
import com.smartshop.customercredit.dto.CustomerCreditFilter;
import com.smartshop.customercredit.entity.CustomerCreditEntry;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class CustomerCreditSpecifications {

    private CustomerCreditSpecifications() {
    }

    public static Specification<CustomerCreditEntry> matching(CustomerCreditFilter filter, LocalDate today) {
        List<Specification<CustomerCreditEntry>> specs = new ArrayList<>();
        if (StringUtils.hasText(filter.q())) {
            String pattern = SearchPatterns.containsPattern(filter.q());
            String phonePattern = SearchPatterns.containsPattern(filter.q().replaceAll("\\s+", ""));
            specs.add((root, query, cb) -> {
                Join<CustomerCreditEntry, Customer> customer = root.join("customer", JoinType.LEFT);
                return cb.or(
                        SearchPatterns.containsIgnoreCase(cb, root.get("reference"), pattern),
                        SearchPatterns.containsIgnoreCase(cb, root.get("description"), pattern),
                        SearchPatterns.containsIgnoreCase(cb, customer.get("lastName"), pattern),
                        SearchPatterns.containsIgnoreCase(cb, customer.get("firstName"), pattern),
                        SearchPatterns.containsIgnoreCase(cb, customer.get("phone"), phonePattern));
            });
        }
        if (filter.customerId() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("customer").get("id"), filter.customerId()));
        }
        if (filter.status() != null) {
            specs.add(CreditStatusSpecifications.hasStatus(filter.status(), today));
        }
        if (Boolean.TRUE.equals(filter.open())) {
            specs.add(CreditStatusSpecifications.open());
        }
        return Specification.allOf(specs);
    }
}
