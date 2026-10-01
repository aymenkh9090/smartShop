package com.smartshop.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

class RoleHierarchyTest {

    private final RoleHierarchy hierarchy = SecurityConfig.roleHierarchy();

    @Test
    void adminInheritsAllRoles() {
        assertThat(reachable("ROLE_ADMIN"))
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_SELLER", "ROLE_TECHNICIAN");
    }

    @Test
    void managerInheritsSellerAndTechnicianButNotAdmin() {
        assertThat(reachable("ROLE_MANAGER"))
                .containsExactlyInAnyOrder("ROLE_MANAGER", "ROLE_SELLER", "ROLE_TECHNICIAN");
    }

    @Test
    void sellerAndTechnicianAreIndependent() {
        assertThat(reachable("ROLE_SELLER")).containsExactly("ROLE_SELLER");
        assertThat(reachable("ROLE_TECHNICIAN")).containsExactly("ROLE_TECHNICIAN");
    }

    private Collection<String> reachable(String role) {
        return hierarchy.getReachableGrantedAuthorities(AuthorityUtils.createAuthorityList(role)).stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }
}
