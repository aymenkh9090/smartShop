package com.smartshop.auth.service;

import com.smartshop.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidRefreshTokenException extends BusinessException {

    public InvalidRefreshTokenException() {
        super("Session expiree ou invalide, veuillez vous reconnecter", HttpStatus.UNAUTHORIZED);
    }
}
