package com.smartshop.customer.dto;

/**
 * @param q texte recherche dans le nom, prenom, telephone, email et CIN
 */
public record CustomerFilter(String q, Boolean active) {
}
