package com.smartshop.common.mapping;

import org.mapstruct.Named;

import java.util.Locale;

/**
 * Normalisation des chaines lors du mapping DTO -> entite.
 * {@link #trimToNull} s'applique automatiquement a toutes les chaines ; les methodes {@code @Named}
 * sont utilisees explicitement via {@code qualifiedByName}.
 */
public final class TextNormalizer {

    private TextNormalizer() {
    }

    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Named("upper")
    public static String upper(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    @Named("lower")
    public static String lower(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    /** Supprime les espaces : "+216 22 333 444" -> "+21622333444". */
    @Named("phone")
    public static String phone(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.replaceAll("\\s+", "");
    }
}
