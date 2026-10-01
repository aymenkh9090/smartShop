package com.smartshop.sale.entity;

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
@Table(name = "sale_item")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SaleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_id", nullable = false, updatable = false)
    private Sale sale;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, updatable = false)
    private Product product;

    @Column(nullable = false, updatable = false)
    private int quantity;

    /** Copie du prix de vente au moment de la vente : l'historique ne change pas si le prix evolue. */
    @Column(name = "unit_price", nullable = false, updatable = false, precision = 12, scale = 3)
    private BigDecimal unitPrice;

    @Column(name = "line_total", nullable = false, updatable = false, precision = 12, scale = 3)
    private BigDecimal lineTotal;

    /** Garantie (mois) applicable a cette vente, copiee depuis le produit. */
    @Column(name = "warranty_months", updatable = false)
    private Integer warrantyMonths;

    /** Unites (IMEI) vendues pour un produit suivi a l'unite. */
    @BatchSize(size = 50)
    @ManyToMany
    @JoinTable(name = "sale_item_unit",
            joinColumns = @JoinColumn(name = "sale_item_id"),
            inverseJoinColumns = @JoinColumn(name = "unit_id"))
    private List<ProductUnit> units = new ArrayList<>();

    SaleItem(Sale sale, Product product, int quantity, List<ProductUnit> units) {
        this.sale = sale;
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = Money.of(product.getSalePrice());
        this.lineTotal = Money.times(unitPrice, quantity);
        this.warrantyMonths = product.getWarrantyMonths();
        this.units = new ArrayList<>(units);
    }
}
