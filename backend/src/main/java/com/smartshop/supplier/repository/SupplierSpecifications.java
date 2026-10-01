package com.smartshop.supplier.repository;

import com.smartshop.common.persistence.SearchPatterns;
import com.smartshop.supplier.dto.SupplierFilter;
import com.smartshop.supplier.entity.Supplier;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class SupplierSpecifications {

    private SupplierSpecifications() {
    }

    public static Specification<Supplier> matching(SupplierFilter filter) {
        List<Specification<Supplier>> specs = new ArrayList<>();
        if (StringUtils.hasText(filter.q())) {
            String pattern = SearchPatterns.containsPattern(filter.q());
            String phonePattern = SearchPatterns.containsPattern(filter.q().replaceAll("\\s+", ""));
            specs.add((root, query, cb) -> cb.or(
                    SearchPatterns.containsIgnoreCase(cb, root.get("name"), pattern),
                    SearchPatterns.containsIgnoreCase(cb, root.get("phone"), phonePattern),
                    SearchPatterns.containsIgnoreCase(cb, root.get("email"), pattern),
                    SearchPatterns.containsIgnoreCase(cb, root.get("taxId"), pattern)));
        }
        if (filter.active() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("active"), filter.active()));
        }
        return Specification.allOf(specs);
    }
}
