package com.smartshop.product.repository;

import com.smartshop.common.persistence.SearchPatterns;
import com.smartshop.product.dto.ProductFilter;
import com.smartshop.product.entity.Product;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> matching(ProductFilter filter) {
        List<Specification<Product>> specs = new ArrayList<>();
        if (StringUtils.hasText(filter.q())) {
            String pattern = SearchPatterns.containsPattern(filter.q());
            specs.add((root, query, cb) -> cb.or(
                    SearchPatterns.containsIgnoreCase(cb, root.get("name"), pattern),
                    SearchPatterns.containsIgnoreCase(cb, root.get("sku"), pattern),
                    SearchPatterns.containsIgnoreCase(cb, root.get("barcode"), pattern),
                    SearchPatterns.containsIgnoreCase(cb, root.get("reference"), pattern),
                    SearchPatterns.containsIgnoreCase(cb, root.get("brand"), pattern),
                    SearchPatterns.containsIgnoreCase(cb, root.get("model"), pattern)));
        }
        if (filter.categoryId() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("category").get("id"), filter.categoryId()));
        }
        if (filter.type() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("type"), filter.type()));
        }
        if (filter.active() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("active"), filter.active()));
        }
        return Specification.allOf(specs);
    }
}
