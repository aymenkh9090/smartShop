package com.smartshop.user.entity;

/**
 * Roles du personnel. La hierarchie (ADMIN > MANAGER > SELLER / TECHNICIAN) est definie dans SecurityConfig.
 */
public enum Role {
    ADMIN,
    MANAGER,
    SELLER,
    TECHNICIAN
}
