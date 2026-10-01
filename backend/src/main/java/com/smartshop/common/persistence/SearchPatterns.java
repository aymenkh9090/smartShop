package com.smartshop.common.persistence;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

import java.util.Locale;

/**
 * Recherche "contient", insensible a la casse, avec echappement de % et _.
 */
public final class SearchPatterns {

    private static final char ESCAPE = '\\';

    private SearchPatterns() {
    }

    public static String containsPattern(String text) {
        String escaped = text.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    public static Predicate containsIgnoreCase(CriteriaBuilder cb, Expression<String> field, String pattern) {
        return cb.like(cb.lower(field), pattern, ESCAPE);
    }
}
