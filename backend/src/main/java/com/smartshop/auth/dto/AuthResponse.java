package com.smartshop.auth.dto;

import com.smartshop.user.dto.UserResponse;

/**
 * @param expiresIn duree de validite de l'access token, en secondes
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {

    public static final String BEARER = "Bearer";
}
