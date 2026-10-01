package com.smartshop.purchase.entity;

import com.smartshop.common.payment.Money;
import com.smartshop.product.entity.Product;
import com.smartshop.stock.entity.ProductUnit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "purchase_item")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_id", nullable = false, updatable = false)
    private Purchase purchase;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, updatable = false)
    private Product product;

    @Column(nullable = false, updatable = false)
    private int quantity;

    @Column(name = "unit_cost", nullable = false, updatable = false, precision = 12, scale = 3)
    private BigDecimal unitCost;

    @Column(name = "line_total", nullable = false, updatable = false, precision = 12, scale = 3)
    private BigDecimal lineTotal;

    /** Unites (IMEI) recues pour un produit suivi a l'unite. */
    @BatchSize(size = 50)
    @ManyToMany
    @JoinTable(name = "purchase_item_unit",
            joinColumns = @JoinColumn(name = "purchase_item_id"),
            inverseJoinColumns = @JoinColumn(name = "unit_id"))
    private List<ProductUnit> units = new ArrayList<>();

    PurchaseItem(Purchase purchase, Product product, int quantity, BigDecimal unitCost, List<ProductUnit> units) {
        this.purchase = purchase;
        this.product = product;
        this.quantity = quantity;
        this.unitCost = Money.of(unitCost);
        this.lineTotal = Money.times(this.unitCost, quantity);
        this.units = new ArrayList<>(units);
    }
}
