package com.smartshop.dashboard.service;

import com.smartshop.aftersales.entity.AfterSalesStatus;
import com.smartshop.config.BusinessCalendar;
import com.smartshop.dashboard.dto.DashboardResponse;
import com.smartshop.dashboard.dto.DashboardResponse.AfterSales;
import com.smartshop.dashboard.dto.DashboardResponse.DailyAmount;
import com.smartshop.dashboard.dto.DashboardResponse.LowStock;
import com.smartshop.dashboard.dto.DashboardResponse.MonthTotals;
import com.smartshop.dashboard.dto.DashboardResponse.Totals;
import com.smartshop.dashboard.repository.DashboardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tableau de bord : chaque section n'est calculee que si l'un des roles de l'utilisateur (hierarchie comprise)
 * y a acces. Matrice : MANAGER (et ADMIN) tout ; SELLER ventes du jour ; SELLER et TECHNICIAN SAV ;
 * stock faible pour tous.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    static final int CHART_DAYS = 30;
    static final int LIST_SIZE = 5;
    static final int READY_LIST_SIZE = 10;

    private static final String MANAGER = "ROLE_MANAGER";
    private static final String SELLER = "ROLE_SELLER";
    private static final String TECHNICIAN = "ROLE_TECHNICIAN";

    private final DashboardRepository repository;
    private final BusinessCalendar calendar;
    private final RoleHierarchy roleHierarchy;

    @Transactional(readOnly = true)
    public DashboardResponse dashboard(Collection<? extends GrantedAuthority> authorities) {
        Set<String> roles = AuthorityUtils.authorityListToSet(roleHierarchy.getReachableGrantedAuthorities(authorities));
        boolean manager = roles.contains(MANAGER);
        boolean seller = roles.contains(SELLER);
        boolean afterSalesAccess = seller || roles.contains(TECHNICIAN);

        LocalDate today = calendar.today();
        LocalDate monthStart = today.withDayOfMonth(1);

        Totals todaySales = seller ? repository.sales(calendar.startOf(today), calendar.startOf(today.plusDays(1))) : null;
        MonthTotals monthSales = manager ? monthSales(today, monthStart) : null;
        Totals monthPurchases = manager ? repository.purchases(calendar.startOf(monthStart), calendar.startOf(today.plusDays(1))) : null;
        LowStock lowStock = new LowStock(repository.countLowStock(), repository.lowStockProducts(LIST_SIZE));
        AfterSales afterSales = afterSalesAccess ? afterSales() : null;
        Totals monthAfterSales = manager
                ? repository.deliveredAfterSales(calendar.startOf(monthStart), calendar.startOf(today.plusDays(1)))
                : null;

        return new DashboardResponse(
                today,
                todaySales,
                monthSales,
                monthPurchases,
                lowStock,
                manager ? repository.customerCredits(today) : null,
                manager ? repository.supplierCredits(today) : null,
                afterSales,
                monthAfterSales,
                manager ? repository.topProducts(calendar.startOf(monthStart), calendar.startOf(today.plusDays(1)), LIST_SIZE) : null,
                manager ? dailyRevenue(today) : null);
    }

    /** Mois en cours compare a la meme periode du mois precedent (du 1er au meme jour, borne a la fin du mois). */
    private MonthTotals monthSales(LocalDate today, LocalDate monthStart) {
        Totals current = repository.sales(calendar.startOf(monthStart), calendar.startOf(today.plusDays(1)));
        // minusMonths borne au dernier jour du mois (31/03 -> 28/02) : la periode precedente ne depasse jamais monthStart
        Totals previous = repository.sales(calendar.startOf(monthStart.minusMonths(1)),
                calendar.startOf(today.minusMonths(1).plusDays(1)));
        return new MonthTotals(current.count(), current.amount(), previous.amount());
    }

    private AfterSales afterSales() {
        Map<AfterSalesStatus, Long> byStatus = repository.openTicketsByStatus();
        long open = byStatus.values().stream().mapToLong(Long::longValue).sum();
        return new AfterSales(open, byStatus, repository.readyTickets(READY_LIST_SIZE));
    }

    private List<DailyAmount> dailyRevenue(LocalDate today) {
        return repository.dailySales(today.minusDays(CHART_DAYS - 1L), today, calendar.zone()).entrySet().stream()
                .map(entry -> new DailyAmount(entry.getKey(), entry.getValue()))
                .toList();
    }
}
