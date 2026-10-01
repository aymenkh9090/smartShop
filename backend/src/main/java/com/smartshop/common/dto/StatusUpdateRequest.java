package com.smartshop.common.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Active ou desactive une ressource referencee par l'historique (produit, client, fournisseur)
 * au lieu de la supprimer.
 */
public record StatusUpdateRequest(@NotNull Boolean active) {
}
