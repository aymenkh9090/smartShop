package com.smartshop.user.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Compte administrateur cree au premier demarrage (base sans utilisateur).
 * Le mot de passe vient de ADMIN_PASSWORD et n'a pas de valeur par defaut.
 */
@ConfigurationProperties(prefix = "smartshop.bootstrap-admin")
public record BootstrapAdminProperties(String username, String email, String password) {

    @Override
    public String toString() {
        return "BootstrapAdminProperties[username=%s, email=%s]".formatted(username, email);
    }
}
