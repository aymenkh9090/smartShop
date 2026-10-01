package com.smartshop.purchase.repository;

import com.smartshop.common.persistence.SearchPatterns;
import com.smartshop.purchase.dto.PurchaseFilter;
import com.smartshop.purchase.entity.Purchase;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class PurchaseSpecifications {

    private PurchaseSpecifications() {
    }

    public static Specification<Purchase> matching(PurchaseFilter filter) {
        List<Specification<Purchase>> specs = new ArrayList<>();
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
