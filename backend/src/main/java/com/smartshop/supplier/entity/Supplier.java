package com.smartshop.supplier.entity;

import com.smartshop.common.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "supplier")
@NoArgsConstructor
public class Supplier extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    /** Raison sociale, unique sans tenir compte de la casse. */
    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(length = 150)
    private String email;

    @Column(length = 255)
    private String address;

    /** Matricule fiscal. */
    @Column(name = "tax_id", unique = true, length = 30)
    private String taxId;

    @Column(length = 1000)
    private String notes;

    @Column(nullable = false)
    private boolean active = true;
}
