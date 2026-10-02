package com.smartshop.dashboard.service;

import com.smartshop.config.BusinessCalendar;
import com.smartshop.dashboard.dto.DashboardResponse;
import com.smartshop.dashboard.dto.DashboardResponse.Credits;
import com.smartshop.dashboard.dto.DashboardResponse.Totals;
import com.smartshop.dashboard.repository.DashboardRepository;
import com.smartshop.security.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DashboardServiceTest {

    private static final ZoneId TUNIS = ZoneId.of("Africa/Tunis");

    @Mock
    private DashboardRepository repository;

    private DashboardService service;

    @BeforeEach
    void setUp() {
        service = service(Instant.parse("2026-10-15T09:00:00Z"));
        Totals empty = new Totals(0, BigDecimal.ZERO);
        when(repository.sales(any(), any())).thenReturn(empty);
        when(repository.purchases(any(), any())).thenReturn(empty);
        when(repository.deliveredAfterSales(any(), any())).thenReturn(empty);
        when(repository.customerCredits(any())).thenReturn(new Credits(0, BigDecimal.ZERO, BigDecimal.ZERO));
        when(repository.supplierCredits(any())).thenReturn(new Credits(0, BigDecimal.ZERO, BigDecimal.ZERO));
        when(repository.openTicketsByStatus()).thenReturn(Map.of());
        when(repository.dailySales(any(), any(), any())).thenReturn(new LinkedHashMap<>());
    }

    @Test
    void seller_seesTodaySalesStockAndAfterSales_only() {
        DashboardResponse response = service.dashboard(AuthorityUtils.createAuthorityList("ROLE_SELLER"));

        assertThat(response.todaySales()).isNotNull();
        assertThat(response.lowStock()).isNotNull();
        assertThat(response.afterSales()).isNotNull();
        assertThat(response.monthSales()).isNull();
        assertThat(response.monthPurchases()).isNull();
        assertThat(response.customerCredits()).isNull();
        assertThat(response.supplierCredits()).isNull();
        assertThat(response.monthAfterSales()).isNull();
        assertThat(response.topProducts()).isNull();
        assertThat(response.dailyRevenue()).isNull();
        verify(repository, never()).customerCredits(any());
        verify(repository, never()).topProducts(any(), any(), anyInt());
    }

    @Test
    void technician_seesAfterSalesAndStock_butNoSales() {
        DashboardResponse response = service.dashboard(AuthorityUtils.createAuthorityList("ROLE_TECHNICIAN"));

        assertThat(response.afterSales()).isNotNull();
        assertThat(response.lowStock()).isNotNull();
        assertThat(response.todaySales()).isNull();
        assertThat(response.monthSales()).isNull();
        verify(repository, never()).sales(any(), any());
    }

    @Test
    void admin_inheritsEverySection() {
        DashboardResponse response = service.dashboard(AuthorityUtils.createAuthorityList("ROLE_ADMIN"));

        assertThat(response.todaySales()).isNotNull();
        assertThat(response.monthSales()).isNotNull();
        assertThat(response.monthPurchases()).isNotNull();
        assertThat(response.customerCredits()).isNotNull();
        assertThat(response.supplierCredits()).isNotNull();
        assertThat(response.afterSales()).isNotNull();
        assertThat(response.monthAfterSales()).isNotNull();
        assertThat(response.topProducts()).isNotNull();
        assertThat(response.dailyRevenue()).isNotNull();
    }

    @Test
    void periods_followTheShopTimeZone_andCompareWithTheSamePeriodOfLastMonth() {
        // 31/03 a 23:30 heure de Tunis (UTC+1)
        service = service(Instant.parse("2026-03-31T22:30:00Z"));

        service.dashboard(AuthorityUtils.createAuthorityList("ROLE_MANAGER"));

        // Jour : [31/03 00:00, 01/04 00:00[ heure de Tunis
        verify(repository).sales(at("2026-03-31"), at("2026-04-01"));
        // Mois : [01/03, 01/04[ ; mois precedent : [01/02, 01/03[ (31/03 - 1 mois = 28/02, fin incluse)
        verify(repository).sales(at("2026-03-01"), at("2026-04-01"));
        verify(repository).sales(at("2026-02-01"), at("2026-03-01"));
        verify(repository).dailySales(LocalDate.parse("2026-03-02"), LocalDate.parse("2026-03-31"), TUNIS);
    }

    @Test
    void today_isTheShopDate_evenWhenUtcIsStillTheDayBefore() {
        // 30/04 23:30 UTC = 01/05 00:30 a Tunis : nouveau jour ET nouveau mois
        service = service(Instant.parse("2026-04-30T23:30:00Z"));

        DashboardResponse response = service.dashboard(AuthorityUtils.createAuthorityList("ROLE_MANAGER"));

        assertThat(response.date()).isEqualTo(LocalDate.parse("2026-05-01"));
        // Le 1er du mois, le jour et le mois en cours couvrent la meme periode
        verify(repository, times(2)).sales(at("2026-05-01"), at("2026-05-02"));
        verify(repository).purchases(at("2026-05-01"), at("2026-05-02"));
    }

    @Test
    void afterSales_openCountIsTheSumOfOpenStatuses() {
        when(repository.openTicketsByStatus()).thenReturn(Map.of(
                com.smartshop.aftersales.entity.AfterSalesStatus.RECEIVED, 2L,
                com.smartshop.aftersales.entity.AfterSalesStatus.READY, 3L));
        when(repository.readyTickets(anyInt())).thenReturn(List.of());

        assertThat(service.dashboard(AuthorityUtils.createAuthorityList("ROLE_TECHNICIAN")).afterSales().openCount())
                .isEqualTo(5);
    }

    private DashboardService service(Instant now) {
        RoleHierarchy hierarchy = (RoleHierarchy) ReflectionTestUtils.invokeMethod(SecurityConfig.class, "roleHierarchy");
        return new DashboardService(repository, new BusinessCalendar(Clock.fixed(now, ZoneOffset.UTC), TUNIS), hierarchy);
    }

    private static Instant at(String date) {
        return LocalDate.parse(date).atStartOfDay(TUNIS).toInstant();
    }
}
