package com.smartshop.stock.entity;

import com.smartshop.common.entity.AuditableEntity;
import com.smartshop.product.entity.Product;
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

/**
 * Exemplaire physique d'un produit suivi a l'unite (smartphone, montre...).
 */
@Getter
@Entity
@Table(name = "product_unit")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductUnit extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(unique = true, length = 15)
    private String imei;

    @Column(name = "serial_number", length = 50)
    private String serialNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductUnitStatus status;

    public ProductUnit(Product product, String imei, String serialNumber) {
        this.product = product;
        this.imei = imei;
        this.serialNumber = serialNumber;
        this.status = ProductUnitStatus.IN_STOCK;
    }

    public boolean isInStock() {
        return status == ProductUnitStatus.IN_STOCK;
    }

    public void changeStatus(ProductUnitStatus status) {
        this.status = status;
    }

    /** Libelle pour les messages : IMEI de preference, sinon numero de serie. */
    public String label() {
        return imei != null ? imei : serialNumber;
    }
}
