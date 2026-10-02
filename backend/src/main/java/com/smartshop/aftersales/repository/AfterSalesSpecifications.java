package com.smartshop.aftersales.repository;

import com.smartshop.aftersales.dto.AfterSalesFilter;
import com.smartshop.aftersales.entity.AfterSalesStatus;
import com.smartshop.aftersales.entity.AfterSalesTicket;
import com.smartshop.common.persistence.SearchPatterns;
import com.smartshop.customer.entity.Customer;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class AfterSalesSpecifications {

    private static final List<AfterSalesStatus> CLOSED = List.of(AfterSalesStatus.DELIVERED, AfterSalesStatus.CANCELLED);

    private AfterSalesSpecifications() {
    }

    public static Specification<AfterSalesTicket> matching(AfterSalesFilter filter) {
        List<Specification<AfterSalesTicket>> specs = new ArrayList<>();
        if (StringUtils.hasText(filter.q())) {
            String pattern = SearchPatterns.containsPattern(filter.q());
            String compactPattern = SearchPatterns.containsPattern(filter.q().replaceAll("\\s+", ""));
            specs.add((root, query, cb) -> {
                Join<AfterSalesTicket, Customer> customer = root.join("customer");
                return cb.or(
                        SearchPatterns.containsIgnoreCase(cb, root.get("ticketNumber"), pattern),
                        SearchPatterns.containsIgnoreCase(cb, root.get("imei"), compactPattern),
                        SearchPatterns.containsIgnoreCase(cb, root.get("serialNumber"), pattern),
                        SearchPatterns.containsIgnoreCase(cb, root.get("deviceDescription"), pattern),
                        SearchPatterns.containsIgnoreCase(cb, customer.get("lastName"), pattern),
                        SearchPatterns.containsIgnoreCase(cb, customer.get("firstName"), pattern),
                        SearchPatterns.containsIgnoreCase(cb, customer.get("phone"), compactPattern));
            });
        }
        if (filter.status() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("status"), filter.status()));
        }
        if (filter.technicianId() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("technician").get("id"), filter.technicianId()));
        }
        if (filter.customerId() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("customer").get("id"), filter.customerId()));
        }
        if (filter.open() != null) {
            specs.add((root, query, cb) -> filter.open()
                    ? root.get("status").in(CLOSED).not()
                    : root.get("status").in(CLOSED));
        }
        if (filter.from() != null) {
            specs.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("receivedAt"), filter.from()));
        }
        if (filter.to() != null) {
            specs.add((root, query, cb) -> cb.lessThan(root.get("receivedAt"), filter.to()));
        }
        return Specification.allOf(specs);
    }
}
