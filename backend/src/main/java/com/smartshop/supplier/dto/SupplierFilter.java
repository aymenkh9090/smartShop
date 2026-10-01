package com.smartshop.supplier.dto;

/**
 * @param q texte recherche dans la raison sociale, telephone, email et matricule fiscal
 */
public record SupplierFilter(String q, Boolean active) {
}
