package com.smartshop.customer.entity;

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
@Table(name = "customer")
@NoArgsConstructor
public class Customer extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    /** Unique : sert a retrouver le client au comptoir. Stocke sans espaces. */
    @Column(nullable = false, unique = true, length = 20)
    private String phone;

    @Column(length = 150)
    private String email;

    @Column(length = 255)
    private String address;

    /** Carte d'identite nationale, facultative. */
    @Column(unique = true, length = 20)
    private String cin;

    @Column(length = 1000)
    private String notes;

    @Column(nullable = false)
    private boolean active = true;
}
