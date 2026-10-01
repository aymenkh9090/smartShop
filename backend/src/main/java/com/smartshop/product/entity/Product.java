package com.smartshop.product.entity;

import com.smartshop.category.entity.Category;
import com.smartshop.common.entity.AuditableEntity;
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
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "product")
@NoArgsConstructor
public class Product extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    /** Code produit interne, unique. */
    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @Column(unique = true, length = 50)
    private String barcode;

    /** Reference fabricant (ex. SM-A546B). */
    @Column(length = 100)
    private String reference;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProductType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_condition", nullable = false, length = 20)
    private ProductCondition condition;

    @Column(length = 100)
    private String brand;

    @Column(length = 100)
    private String model;

    @Column(name = "purchase_price", nullable = false, precision = 12, scale = 3)
    private BigDecimal purchasePrice;

    @Column(name = "sale_price", nullable = false, precision = 12, scale = 3)
    private BigDecimal salePrice;

    /** Jamais modifiee directement : uniquement via les mouvements de stock. */
    @Setter(AccessLevel.NONE)
    @Column(nullable = false)
    private int quantity;

    @Column(name = "min_stock", nullable = false)
    private int minStock;

    /** Produit suivi a l'unite (IMEI / numero de serie). */
    @Column(name = "serial_tracked", nullable = false)
    private boolean serialTracked;

    /** Garantie par defaut, en mois. */
    @Column(name = "warranty_months")
    private Integer warrantyMonths;

    @Column(nullable = false)
    private boolean active = true;

    public boolean isLowStock() {
        return quantity <= minStock;
    }
}
