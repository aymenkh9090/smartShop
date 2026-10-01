package com.smartshop.security;

import com.smartshop.user.entity.User;
import com.smartshop.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Utilisateur authentifie de la requete courante, pour tracer l'auteur des operations.
 */
@Component
@RequiredArgsConstructor
public class CurrentUserProvider {

    private final UserRepository userRepository;

    /** Reference JPA (sans requete) vers l'utilisateur identifie par le claim "uid" du JWT. */
    public User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication
                && jwtAuthentication.getToken().getClaim(JwtService.USER_ID_CLAIM) instanceof Number userId) {
            return userRepository.getReferenceById(userId.longValue());
        }
        throw new IllegalStateException("Aucun utilisateur authentifie par JWT");
    }
}
