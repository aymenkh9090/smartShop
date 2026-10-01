package com.smartshop.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Parametres JWT. Le secret est obligatoire et fourni par la variable d'environnement JWT_SECRET :
 * l'application refuse de demarrer s'il est absent ou trop court (HS256 exige au moins 256 bits).
 */
@Validated
@ConfigurationProperties(prefix = "smartshop.security.jwt")
public record JwtProperties(
        @NotBlank @Size(min = 32, message = "doit contenir au moins 32 caracteres") String secret,
        @NotNull Duration accessTokenTtl,
        @NotNull Duration refreshTokenTtl
) {
}
