package com.smartshop.aftersales.entity;

import java.util.Set;

/**
 * Cycle de vie d'un ticket SAV. Les transitions autorisees sont definies ici et verifiees par le ticket.
 */
public enum AfterSalesStatus {
    RECEIVED,
    DIAGNOSING,
    WAITING_FOR_PART,
    IN_REPAIR,
    READY,
    DELIVERED,
    CANCELLED;

    /**
     * Transitions du workflow technique. La livraison ({@code DELIVERED}) et l'annulation ({@code CANCELLED})
     * sont des operations dediees, avec leurs propres controles.
     */
    public Set<AfterSalesStatus> nextWorkflowStatuses() {
        return switch (this) {
            case RECEIVED -> Set.of(DIAGNOSING);
            // READY directement : appareil sans panne ou non reparable, a rendre au client
            case DIAGNOSING -> Set.of(WAITING_FOR_PART, IN_REPAIR, READY);
            case WAITING_FOR_PART -> Set.of(IN_REPAIR);
            case IN_REPAIR -> Set.of(WAITING_FOR_PART, READY);
            // Reprise si le controle a la remise revele un probleme
            case READY -> Set.of(IN_REPAIR);
            case DELIVERED, CANCELLED -> Set.of();
        };
    }

    public boolean isClosed() {
        return this == DELIVERED || this == CANCELLED;
    }

    /** Etapes ou le technicien travaille sur l'appareil : pieces et diagnostic modifiables. */
    public boolean isInWorkshop() {
        return this == DIAGNOSING || this == WAITING_FOR_PART || this == IN_REPAIR;
    }
}
