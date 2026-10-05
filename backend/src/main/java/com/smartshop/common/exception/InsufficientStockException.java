package com.smartshop.common.exception;

public class InsufficientStockException extends BusinessException {

    public InsufficientStockException(String productName, int available, int requested) {
        super("Stock insuffisant pour '%s' : %d disponible(s), %d demandé(s)"
                .formatted(productName, available, requested));
    }
}
