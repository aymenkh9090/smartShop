package com.smartshop.aftersales.entity;

import com.smartshop.common.payment.Money;
import com.smartshop.product.entity.Product;
import com.smartshop.stock.entity.ProductUnit;
import com.smartshop.user.entity.User;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Piece utilisee pour une reparation, sortie du stock sous la reference du ticket. */
@Getter
@Entity
@Table(name = "after_sales_part")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AfterSalesPart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false, updatable = false)
    private AfterSalesTicket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, updatable = false)
    private Product product;

    @Column(nullable = false, updatable = false)
    private int quantity;

    /** Copie du prix de vente au moment de l'utilisation. */
    @Column(name = "unit_price", nullable = false, updatable = false, precision = 12, scale = 3)
    private BigDecimal unitPrice;

    @Column(name = "line_total", nullable = false, updatable = false, precision = 12, scale = 3)
    private BigDecimal lineTotal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "added_by", nullable = false, updatable = false)
    private User addedBy;

    @Column(name = "added_at", nullable = false, updatable = false)
    private Instant addedAt;

    /** Unites (IMEI / numero de serie) utilisees pour une piece suivie a l'unite. */
    @BatchSize(size = 50)
    @ManyToMany
    @JoinTable(name = "after_sales_part_unit",
            joinColumns = @JoinColumn(name = "part_id"),
            inverseJoinColumns = @JoinColumn(name = "unit_id"))
    private List<ProductUnit> units = new ArrayList<>();

    AfterSalesPart(AfterSalesTicket ticket, Product product, int quantity, List<ProductUnit> units,
                   User addedBy, Instant addedAt) {
        this.ticket = ticket;
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = Money.of(product.getSalePrice());
        this.lineTotal = Money.times(unitPrice, quantity);
        this.units = new ArrayList<>(units);
        this.addedBy = addedBy;
        this.addedAt = addedAt;
    }
}
