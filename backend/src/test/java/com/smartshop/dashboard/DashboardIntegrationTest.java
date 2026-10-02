package com.smartshop.dashboard;

import com.jayway.jsonpath.JsonPath;
import com.smartshop.AbstractIntegrationTest;
import com.smartshop.config.BusinessCalendar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import java.sql.Timestamp;
import java.time.LocalDate;

import static com.smartshop.user.entity.Role.ADMIN;
import static com.smartshop.user.entity.Role.MANAGER;
import static com.smartshop.user.entity.Role.SELLER;
import static com.smartshop.user.entity.Role.TECHNICIAN;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Jeu de donnees : ventes du jour (dont une annulee), une vente d'hier, une du mois precedent,
 * un credit client et un credit fournisseur en retard, des tickets SAV (recu, pret, livre).
 */
class DashboardIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private BusinessCalendar calendar;

    private LocalDate today;
    private long cableId;

    @BeforeEach
    void setUp() throws Exception {
        jdbcTemplate.execute("""
                truncate table after_sales_payment, after_sales_part_unit, after_sales_part, after_sales_status_change,
                after_sales_ticket, sale_payment, sale_item_unit, sale_item, sale, purchase_payment, purchase_item_unit,
                purchase_item, purchase, stock_movement_unit, stock_movement, product_unit, product, category,
                customer, supplier restart identity cascade""");
        today = calendar.today();
        long categoryId = id(postAs(MANAGER, "/api/categories", "{\"name\":\"Accessoires\"}"));
        long supplierId = id(postAs(MANAGER, "/api/suppliers", "{\"name\":\"TechDistrib\"}"));
        long customerId = id(postAs(SELLER, "/api/customers",
                "{\"firstName\":\"Ali\",\"lastName\":\"Ben Salah\",\"phone\":\"22333444\"}"));
        cableId = id(postAs(MANAGER, "/api/products", productJson("CAB-USB-C", categoryId, "20", 5)));
        long chargerId = id(postAs(MANAGER, "/api/products", productJson("CHG-20W", categoryId, "50", 1)));

        // Achat a credit : 6 x 8 + 10 x 30 = 348, 20 payes
        long purchaseId = id(postAs(MANAGER, "/api/purchases", """
                {"supplierId":%d,"items":[{"productId":%d,"quantity":6,"unitCost":8},
                 {"productId":%d,"quantity":10,"unitCost":30}],"payments":[{"amount":20,"method":"CASH"}]}
                """.formatted(supplierId, cableId, chargerId)));
        jdbcTemplate.update("update purchase set due_date = ? where id = ?", today.minusDays(2), purchaseId);

        sale(null, cableId, 4, 80);                                    // aujourd'hui : 80
        long creditSale = sale(customerId, chargerId, 2, 30);          // aujourd'hui : 100, 70 dus
        jdbcTemplate.update("update sale set due_date = ? where id = ?", today.minusDays(1), creditSale);
        long cancelled = sale(null, chargerId, 1, 50);                 // annulee : exclue
        postAs(MANAGER, "/api/sales/%d/cancel".formatted(cancelled), "{\"reason\":\"Erreur\"}")
                .andExpect(status().isOk());
        backdate(sale(null, chargerId, 1, 50), today.minusDays(1));   // hier : 50
        backdate(sale(null, cableId, 1, 20), today.minusMonths(1));   // mois precedent : 20

        // SAV : un ticket recu, un pret (60 dus), un livre (40 factures)
        ticket(customerId);
        repairToReady(ticket(customerId), 60);
        long delivered = ticket(customerId);
        repairToReady(delivered, 40);
        postAs(SELLER, "/api/after-sales/%d/deliver".formatted(delivered),
                "{\"payments\":[{\"amount\":40,\"method\":\"CASH\"}]}").andExpect(status().isOk());
    }

    @Test
    void manager_getsEveryIndicator() throws Exception {
        boolean yesterdayInMonth = today.getDayOfMonth() > 1;
        getAs(MANAGER, "/api/dashboard")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value(today.toString()))
                // ventes : l'annulee est exclue
                .andExpect(jsonPath("$.todaySales.count").value(2))
                .andExpect(jsonPath("$.todaySales.amount").value(180.0))
                .andExpect(jsonPath("$.monthSales.count").value(yesterdayInMonth ? 3 : 2))
                .andExpect(jsonPath("$.monthSales.amount").value(yesterdayInMonth ? 230.0 : 180.0))
                .andExpect(jsonPath("$.monthSales.previousAmount").value(20.0))
                .andExpect(jsonPath("$.monthPurchases.count").value(1))
                .andExpect(jsonPath("$.monthPurchases.amount").value(348.0))
                // stock : 6 - 4 - 1 = 1 cable pour un seuil de 5
                .andExpect(jsonPath("$.lowStock.count").value(1))
                .andExpect(jsonPath("$.lowStock.products[0].id").value(cableId))
                .andExpect(jsonPath("$.lowStock.products[0].quantity").value(1))
                // credits
                .andExpect(jsonPath("$.customerCredits.count").value(1))
                .andExpect(jsonPath("$.customerCredits.outstanding").value(70.0))
                .andExpect(jsonPath("$.customerCredits.overdue").value(70.0))
                .andExpect(jsonPath("$.supplierCredits.outstanding").value(328.0))
                .andExpect(jsonPath("$.supplierCredits.overdue").value(328.0))
                // SAV
                .andExpect(jsonPath("$.afterSales.openCount").value(2))
                .andExpect(jsonPath("$.afterSales.byStatus.RECEIVED").value(1))
                .andExpect(jsonPath("$.afterSales.byStatus.READY").value(1))
                .andExpect(jsonPath("$.afterSales.byStatus.IN_REPAIR").value(0))
                .andExpect(jsonPath("$.afterSales.ready.length()").value(1))
                .andExpect(jsonPath("$.afterSales.ready[0].amountDue").value(60.0))
                .andExpect(jsonPath("$.afterSales.ready[0].customerName").value("Ben Salah Ali"))
                .andExpect(jsonPath("$.monthAfterSales.count").value(1))
                .andExpect(jsonPath("$.monthAfterSales.amount").value(40.0))
                // top produits du mois : le cable (4) avant le chargeur (2 ou 3)
                .andExpect(jsonPath("$.topProducts[0].sku").value("CAB-USB-C"))
                .andExpect(jsonPath("$.topProducts[0].quantity").value(4))
                .andExpect(jsonPath("$.topProducts[0].revenue").value(80.0))
                .andExpect(jsonPath("$.topProducts[1].sku").value("CHG-20W"))
                // courbe : 30 jours, jours sans vente a 0
                .andExpect(jsonPath("$.dailyRevenue.length()").value(30))
                .andExpect(jsonPath("$.dailyRevenue[29].date").value(today.toString()))
                .andExpect(jsonPath("$.dailyRevenue[29].amount").value(180.0))
                .andExpect(jsonPath("$.dailyRevenue[28].amount").value(50.0))
                .andExpect(jsonPath("$.dailyRevenue[0].amount").value(0.0));
    }

    @Test
    void admin_inheritsTheManagerDashboard() throws Exception {
        getAs(ADMIN, "/api/dashboard")
                .andExpect(jsonPath("$.monthSales.count").exists())
                .andExpect(jsonPath("$.supplierCredits.count").value(1));
    }

    @Test
    void seller_seesOnlyTodaySalesStockAndAfterSales() throws Exception {
        getAs(SELLER, "/api/dashboard")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todaySales.amount").value(180.0))
                .andExpect(jsonPath("$.lowStock.count").value(1))
                .andExpect(jsonPath("$.afterSales.openCount").value(2))
                .andExpect(jsonPath("$.monthSales").value(nullValue()))
                .andExpect(jsonPath("$.monthPurchases").value(nullValue()))
                .andExpect(jsonPath("$.customerCredits").value(nullValue()))
                .andExpect(jsonPath("$.supplierCredits").value(nullValue()))
                .andExpect(jsonPath("$.monthAfterSales").value(nullValue()))
                .andExpect(jsonPath("$.topProducts").value(nullValue()))
                .andExpect(jsonPath("$.dailyRevenue").value(nullValue()));
    }

    @Test
    void technician_seesAfterSalesAndStock_butNoFigures() throws Exception {
        getAs(TECHNICIAN, "/api/dashboard")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.afterSales.ready[0].ticketNumber").exists())
                .andExpect(jsonPath("$.lowStock.count").value(1))
                .andExpect(jsonPath("$.todaySales").value(nullValue()))
                .andExpect(jsonPath("$.monthSales").value(nullValue()))
                .andExpect(jsonPath("$.customerCredits").value(nullValue()));
    }

    @Test
    void anonymous_isRejected() throws Exception {
        mockMvc.perform(get("/api/dashboard")).andExpect(status().isUnauthorized());
    }

    // ---------- outils ----------

    private long sale(Long customerId, long productId, int quantity, int paid) throws Exception {
        return id(postAs(SELLER, "/api/sales", """
                {"customerId":%s,"items":[{"productId":%d,"quantity":%d}],"payments":[{"amount":%d,"method":"CASH"}]}
                """.formatted(customerId, productId, quantity, paid)));
    }

    /** Une heure apres le debut du jour (heure de la boutique) : toujours dans la bonne journee. */
    private void backdate(long saleId, LocalDate day) {
        jdbcTemplate.update("update sale set created_at = ? where id = ?",
                Timestamp.from(calendar.startOf(day).plusSeconds(3600)), saleId);
    }

    private long ticket(long customerId) throws Exception {
        return id(postAs(SELLER, "/api/after-sales", """
                {"customerId":%d,"deviceDescription":"Samsung A55","reportedIssue":"Ecran"}""".formatted(customerId)));
    }

    private void repairToReady(long ticketId, int laborCost) throws Exception {
        postAs(TECHNICIAN, "/api/after-sales/%d/status".formatted(ticketId), "{\"status\":\"DIAGNOSING\"}")
                .andExpect(status().isOk());
        putAs(TECHNICIAN, "/api/after-sales/" + ticketId,
                "{\"diagnosis\":\"Ecran\",\"laborCost\":%d,\"underWarranty\":false}".formatted(laborCost))
                .andExpect(status().isOk());
        postAs(TECHNICIAN, "/api/after-sales/%d/status".formatted(ticketId), "{\"status\":\"READY\"}")
                .andExpect(status().isOk());
    }

    private static long id(ResultActions result) throws Exception {
        String body = result.andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private static String productJson(String sku, long categoryId, String salePrice, int minStock) {
        return """
                {"sku":"%s","name":"Produit %s","categoryId":%d,"type":"OTHER","condition":"NEW",
                 "purchasePrice":1,"salePrice":%s,"minStock":%d,"serialTracked":false}
                """.formatted(sku, sku, categoryId, salePrice, minStock);
    }
}
