package com.smartshop.stock.repository;

import com.smartshop.common.persistence.SearchPatterns;
import com.smartshop.stock.dto.ProductUnitFilter;
import com.smartshop.stock.dto.StockMovementFilter;
import com.smartshop.stock.entity.ProductUnit;
import com.smartshop.stock.entity.StockMovement;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class StockSpecifications {

    private StockSpecifications() {
    }

    public static Specification<StockMovement> movements(StockMovementFilter filter) {
        List<Specification<StockMovement>> specs = new ArrayList<>();
        if (filter.productId() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("product").get("id"), filter.productId()));
        }
        if (filter.type() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("type"), filter.type()));
        }
        if (filter.from() != null) {
            specs.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), filter.from()));
        }
        if (filter.to() != null) {
            specs.add((root, query, cb) -> cb.lessThan(root.get("createdAt"), filter.to()));
        }
        if (StringUtils.hasText(filter.q())) {
            String pattern = SearchPatterns.containsPattern(filter.q());
            specs.add((root, query, cb) -> cb.or(
                    SearchPatterns.containsIgnoreCase(cb, root.get("product").get("name"), pattern),
                    SearchPatterns.containsIgnoreCase(cb, root.get("product").get("sku"), pattern),
                    SearchPatterns.containsIgnoreCase(cb, root.get("reference"), pattern),
                    SearchPatterns.containsIgnoreCase(cb, root.get("reason"), pattern)));
        }
        return Specification.allOf(specs);
    }

    public static Specification<ProductUnit> units(ProductUnitFilter filter) {
        List<Specification<ProductUnit>> specs = new ArrayList<>();
        if (filter.productId() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("product").get("id"), filter.productId()));
        }
        if (filter.status() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("status"), filter.status()));
        }
        if (StringUtils.hasText(filter.q())) {
            String pattern = SearchPatterns.containsPattern(filter.q());
            specs.add((root, query, cb) -> cb.or(
                    SearchPatterns.containsIgnoreCase(cb, root.get("imei"), pattern),
                    SearchPatterns.containsIgnoreCase(cb, root.get("serialNumber"), pattern)));
        }
        return Specification.allOf(specs);
    }
}
