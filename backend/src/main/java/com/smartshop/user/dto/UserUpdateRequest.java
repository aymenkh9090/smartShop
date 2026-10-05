package com.smartshop.user.dto;

import com.smartshop.user.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Le nom d'utilisateur n'est pas modifiable : il sert de sujet au JWT et de trace dans l'historique.
 * Le mot de passe se change via {@link PasswordResetRequest}.
 */
public record UserUpdateRequest(
        @NotBlank @Email @Size(max = 150)
        String email,

        @NotBlank @Size(max = 100)
        String firstName,

        @NotBlank @Size(max = 100)
        String lastName,

        @NotNull
        Role role
) {
}
