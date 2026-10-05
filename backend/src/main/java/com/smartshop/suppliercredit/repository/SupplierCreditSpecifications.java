package com.smartshop.suppliercredit.repository;

import com.smartshop.common.credit.CreditStatusSpecifications;
import com.smartshop.common.persistence.SearchPatterns;
import com.smartshop.suppliercredit.dto.SupplierCreditFilter;
import com.smartshop.suppliercredit.entity.SupplierCreditEntry;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class SupplierCreditSpecifications {

    private SupplierCreditSpecifications() {
    }

    public static Specification<SupplierCreditEntry> matching(SupplierCreditFilter filter, LocalDate today) {
        List<Specification<SupplierCreditEntry>> specs = new ArrayList<>();
        if (StringUtils.hasText(filter.q())) {
            String pattern = SearchPatterns.containsPattern(filter.q());
            specs.add((root, query, cb) -> cb.or(
                    SearchPatterns.containsIgnoreCase(cb, root.get("reference"), pattern),
                    SearchPatterns.containsIgnoreCase(cb, root.get("supplierInvoiceRef"), pattern),
                    SearchPatterns.containsIgnoreCase(cb, root.get("description"), pattern),
                    SearchPatterns.containsIgnoreCase(cb, root.get("supplier").get("name"), pattern)));
        }
        if (filter.supplierId() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("supplier").get("id"), filter.supplierId()));
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
