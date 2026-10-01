package com.smartshop.user.dto;

import com.smartshop.user.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        @NotBlank
        @Size(min = 3, max = 50)
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "lettres, chiffres, '.', '_' ou '-' uniquement")
        String username,

        @NotBlank @Email @Size(max = 150)
        String email,

        // BCrypt ignore tout ce qui depasse 72 octets
        @NotBlank @Size(min = 8, max = 72)
        String password,

        @NotBlank @Size(max = 100)
        String firstName,

        @NotBlank @Size(max = 100)
        String lastName,

        @NotNull
        Role role
) {

    @Override
    public String toString() {
        return "UserCreateRequest[username=%s, email=%s, role=%s]".formatted(username, email, role);
    }
}
