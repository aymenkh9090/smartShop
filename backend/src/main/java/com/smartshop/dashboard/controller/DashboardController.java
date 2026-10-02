package com.smartshop.dashboard.controller;

import com.smartshop.dashboard.dto.DashboardResponse;
import com.smartshop.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tableau de bord : accessible a tout utilisateur authentifie ; le contenu depend du role.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public DashboardResponse dashboard(Authentication authentication) {
        return dashboardService.dashboard(authentication.getAuthorities());
    }
}
