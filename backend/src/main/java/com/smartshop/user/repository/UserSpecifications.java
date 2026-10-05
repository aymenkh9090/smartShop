package com.smartshop.user.repository;

import com.smartshop.common.persistence.SearchPatterns;
import com.smartshop.user.dto.UserFilter;
import com.smartshop.user.entity.User;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<User> matching(UserFilter filter) {
        List<Specification<User>> specs = new ArrayList<>();
        if (StringUtils.hasText(filter.q())) {
            String pattern = SearchPatterns.containsPattern(filter.q());
            specs.add((root, query, cb) -> cb.or(
                    SearchPatterns.containsIgnoreCase(cb, root.get("username"), pattern),
                    SearchPatterns.containsIgnoreCase(cb, root.get("email"), pattern),
                    SearchPatterns.containsIgnoreCase(cb, root.get("firstName"), pattern),
                    SearchPatterns.containsIgnoreCase(cb, root.get("lastName"), pattern)));
        }
        if (filter.role() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("role"), filter.role()));
        }
        if (filter.enabled() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("enabled"), filter.enabled()));
        }
        return Specification.allOf(specs);
    }
}
