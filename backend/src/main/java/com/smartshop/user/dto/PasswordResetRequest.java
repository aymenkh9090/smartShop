package com.smartshop.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetRequest(
        // BCrypt ignore tout ce qui depasse 72 octets
        @NotBlank @Size(min = 8, max = 72)
        String password
) {

    @Override
    public String toString() {
        return "PasswordResetRequest[password=***]";
    }
}
