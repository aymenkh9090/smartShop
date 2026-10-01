package com.smartshop.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Violation d'une regle metier. Le message est destine a l'utilisateur final.
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    public BusinessException(String message) {
        this(message, HttpStatus.CONFLICT);
    }

    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
