package com.smartshop.stock.entity;

import com.smartshop.product.entity.Product;
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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Mouvement de stock : immuable une fois enregistre (aucun setter).
 */
@Getter
@Entity
@Table(name = "stock_movement")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, updatable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private StockMovementType type;

    @Column(name = "quantity_change", nullable = false, updatable = false)
    private int quantityChange;

    @Column(name = "quantity_after", nullable = false, updatable = false)
    private int quantityAfter;

    @Column(length = 255, updatable = false)
    private String reason;

    @Column(length = 100, updatable = false)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Charge par lots pour eviter le N+1 sur les listes paginees. */
    @BatchSize(size = 50)
    @ManyToMany
    @JoinTable(name = "stock_movement_unit",
            joinColumns = @JoinColumn(name = "movement_id"),
            inverseJoinColumns = @JoinColumn(name = "unit_id"))
    private List<ProductUnit> units = new ArrayList<>();

    /**
     * A construire APRES {@link Product#changeQuantity(int)} : la quantite courante du produit
     * est enregistree comme quantite resultante.
     */
    public StockMovement(Product product, StockMovementType type, int quantityChange, String reason,
                         String reference, User createdBy, List<ProductUnit> units) {
        this.product = product;
        this.type = type;
        this.quantityChange = quantityChange;
        this.quantityAfter = product.getQuantity();
        this.reason = reason;
        this.reference = reference;
        this.createdBy = createdBy;
        this.units = new ArrayList<>(units);
    }
}
