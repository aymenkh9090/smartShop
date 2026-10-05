package com.smartshop.user.dto;

import com.smartshop.user.entity.Role;

/**
 * @param q texte recherche dans le nom d'utilisateur, l'email, le nom et le prenom
 */
public record UserFilter(String q, Role role, Boolean enabled) {
}
