package com.smartshop.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param username nom d'utilisateur ou email
 */
public record LoginRequest(
        @NotBlank @Size(max = 150) String username,
        @NotBlank @Size(max = 72) String password
) {

    @Override
    public String toString() {
        return "LoginRequest[username=%s]".formatted(username);
    }
}
