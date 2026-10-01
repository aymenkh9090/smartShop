package com.smartshop.product.dto;

import com.smartshop.product.entity.ProductType;

/**
 * Criteres de recherche (query params). Un critere absent n'est pas applique.
 *
 * @param q texte recherche dans le nom, SKU, code-barres, reference, marque et modele
 */
public record ProductFilter(String q, Long categoryId, ProductType type, Boolean active) {
}
