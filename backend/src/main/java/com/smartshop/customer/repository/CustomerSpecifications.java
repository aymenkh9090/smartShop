package com.smartshop.customer.repository;

import com.smartshop.common.persistence.SearchPatterns;
import com.smartshop.customer.dto.CustomerFilter;
import com.smartshop.customer.entity.Customer;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class CustomerSpecifications {

    private CustomerSpecifications() {
    }

    public static Specification<Customer> matching(CustomerFilter filter) {
        List<Specification<Customer>> specs = new ArrayList<>();
        if (StringUtils.hasText(filter.q())) {
            String pattern = SearchPatterns.containsPattern(filter.q());
            // Le telephone est stocke sans espaces : on cherche aussi la saisie sans espaces
            String phonePattern = SearchPatterns.containsPattern(filter.q().replaceAll("\\s+", ""));
            specs.add((root, query, cb) -> cb.or(
                    SearchPatterns.containsIgnoreCase(cb, root.get("firstName"), pattern),
                    SearchPatterns.containsIgnoreCase(cb, root.get("lastName"), pattern),
                    SearchPatterns.containsIgnoreCase(cb, root.get("phone"), phonePattern),
                    SearchPatterns.containsIgnoreCase(cb, root.get("email"), pattern),
                    SearchPatterns.containsIgnoreCase(cb, root.get("cin"), pattern)));
        }
        if (filter.active() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("active"), filter.active()));
        }
        return Specification.allOf(specs);
    }
}
