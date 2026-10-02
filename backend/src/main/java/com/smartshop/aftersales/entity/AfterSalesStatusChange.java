package com.smartshop.aftersales.entity;

import com.smartshop.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Changement de statut d'un ticket : immuable, l'historique n'est jamais modifie. */
@Getter
@Entity
@Table(name = "after_sales_status_change")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AfterSalesStatusChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false, updatable = false)
    private AfterSalesTicket ticket;

    /** Null pour la creation du ticket. */
    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20, updatable = false)
    private AfterSalesStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20, updatable = false)
    private AfterSalesStatus toStatus;

    @Column(length = 500, updatable = false)
    private String comment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "changed_by", nullable = false, updatable = false)
    private User changedBy;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    AfterSalesStatusChange(AfterSalesTicket ticket, AfterSalesStatus fromStatus, AfterSalesStatus toStatus,
                           String comment, User changedBy, Instant changedAt) {
        this.ticket = ticket;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.comment = comment;
        this.changedBy = changedBy;
        this.changedAt = changedAt;
    }
}
