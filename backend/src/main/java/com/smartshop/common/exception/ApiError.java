package com.smartshop.common.exception;

import java.time.Instant;
import java.util.List;

/**
 * Format unique des reponses d'erreur de l'API.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String message,
        String path,
        List<FieldViolation> errors
) {

    public static ApiError of(int status, String message, String path) {
        return new ApiError(Instant.now(), status, message, path, List.of());
    }

    public static ApiError of(int status, String message, String path, List<FieldViolation> errors) {
        return new ApiError(Instant.now(), status, message, path, errors);
    }

    public record FieldViolation(String field, String message) {
    }
}
