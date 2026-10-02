package com.smartshop.aftersales;

import com.jayway.jsonpath.JsonPath;
import com.smartshop.AbstractIntegrationTest;
import com.smartshop.TestImeis;
import com.smartshop.aftersales.service.AfterSalesService;
import com.smartshop.common.exception.BusinessException;
import com.smartshop.common.payment.PaymentMethod;
import com.smartshop.common.payment.PaymentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static com.smartshop.user.entity.Role.MANAGER;
import static com.smartshop.user.entity.Role.SELLER;
import static com.smartshop.user.entity.Role.TECHNICIAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AfterSalesIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AfterSalesService afterSalesService;

    private final String soldImei = TestImeis.valid(201);
    private final String foreignImei = TestImeis.valid(999);

    private long customerId;
    private long phoneId;
    private long screenId;
    private long saleId;

    @BeforeEach
    void setUp() throws Exception {
        jdbcTemplate.execute("""
                truncate table after_sales_payment, after_sales_part_unit, after_sales_part, after_sales_status_change,
                after_sales_ticket, sale_payment, sale_item_unit, sale_item, sale, purchase_payment, purchase_item_unit,
                purchase_item, purchase, stock_movement_unit, stock_movement, product_unit, product, category,
                customer, supplier restart identity cascade""");
        long categoryId = id(postAs(MANAGER, "/api/categories", "{\"name\":\"Telephonie\"}"));
        long supplierId = id(postAs(MANAGER, "/api/suppliers", "{\"name\":\"TechDistrib\"}"));
        customerId = id(postAs(SELLER, "/api/customers",
                "{\"firstName\":\"Ali\",\"lastName\":\"Ben Salah\",\"phone\":\"22333444\"}"));
        phoneId = id(postAs(MANAGER, "/api/products", productJson("SM-A55", categoryId, "1299.500", true, 12)));
        screenId = id(postAs(MANAGER, "/api/products", productJson("ECR-A55", categoryId, "120", false, null)));
        postAs(MANAGER, "/api/purchases", """
                {"supplierId":%d,"items":[
                  {"productId":%d,"quantity":1,"unitCost":1100,"units":[{"imei":"%s"}]},
                  {"productId":%d,"quantity":5,"unitCost":60}],
                 "payments":[{"amount":1400,"method":"CASH"}]}
                """.formatted(supplierId, phoneId, soldImei, screenId)).andExpect(status().isCreated());
        saleId = id(postAs(SELLER, "/api/sales", """
                {"customerId":%d,"items":[{"productId":%d,"quantity":1,"unitIds":[%d]}],
                 "payments":[{"amount":1299.5,"method":"CASH"}]}
                """.formatted(customerId, phoneId, unitId(soldImei))));
    }

    // ---------- parcours complet ----------

    @Test
    void fullRepair_fromReceptionToDelivery_isTracedAndPaid() throws Exception {
        long ticketId = id(postAs(SELLER, "/api/after-sales", ticketJson(foreignImei))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketNumber").value(matchesPattern("SAV-\\d{4}-\\d{6}")))
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.underWarranty").value(false))
                .andExpect(jsonPath("$.nextStatuses[0]").value("DIAGNOSING")));
        String number = ticketNumber(ticketId);

        // Le technicien qui commence le diagnostic prend le ticket en charge
        moveTo(ticketId, "DIAGNOSING").andExpect(jsonPath("$.technician.username").value("it-technician"));
        putAs(TECHNICIAN, "/api/after-sales/" + ticketId, workJson("Ecran casse", 30, false))
                .andExpect(status().isOk());
        part(ticketId, screenId, 1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parts[0].id").isNumber())
                .andExpect(jsonPath("$.parts[0].lineTotal").value(120.0))
                .andExpect(jsonPath("$.totalCost").value(150.0));
        getAs(MANAGER, "/api/products/" + screenId).andExpect(jsonPath("$.quantity").value(4));
        getAs(MANAGER, "/api/stock/movements?q=" + number)
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].type").value("STOCK_OUT"))
                .andExpect(jsonPath("$.content[0].reason").value("Piece SAV"));

        moveTo(ticketId, "IN_REPAIR").andExpect(status().isOk());
        moveTo(ticketId, "READY").andExpect(status().isOk());
        postAs(SELLER, "/api/after-sales/%d/payments".formatted(ticketId), "{\"amount\":50,\"method\":\"CASH\"}")
                .andExpect(jsonPath("$.amountDue").value(100.0))
                .andExpect(jsonPath("$.paymentStatus").value("PARTIALLY_PAID"));

        postAs(SELLER, "/api/after-sales/%d/deliver".formatted(ticketId), "{}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("Reste a payer")));
        postAs(SELLER, "/api/after-sales/%d/deliver".formatted(ticketId),
                "{\"payments\":[{\"amount\":100,\"method\":\"CARD\"}],\"comment\":\"Teste devant le client\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"))
                .andExpect(jsonPath("$.deliveredAt").isNotEmpty())
                .andExpect(jsonPath("$.paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.payments.length()").value(2))
                .andExpect(jsonPath("$.statusHistory.length()").value(5))
                .andExpect(jsonPath("$.statusHistory[4].toStatus").value("DELIVERED"))
                .andExpect(jsonPath("$.statusHistory[4].changedBy").value("it-seller"))
                .andExpect(jsonPath("$.nextStatuses.length()").value(0));

        // Ticket cloture : plus aucune modification
        postAs(MANAGER, "/api/after-sales/%d/cancel".formatted(ticketId), "{\"reason\":\"x\"}")
                .andExpect(status().isConflict());
    }

    @Test
    void deviceSoldByTheShop_isRecognisedByImei_andRepairedUnderWarranty() throws Exception {
        long ticketId = id(postAs(SELLER, "/api/after-sales", ticketJson(soldImei))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.product.id").value(phoneId))
                .andExpect(jsonPath("$.saleId").value(saleId))
                .andExpect(jsonPath("$.warrantyUntil").value(LocalDate.now(ZoneId.of("Africa/Tunis")).plusMonths(12).toString()))
                .andExpect(jsonPath("$.underWarranty").value(true)));

        moveTo(ticketId, "DIAGNOSING");
        putAs(TECHNICIAN, "/api/after-sales/" + ticketId, workJson("Defaut ecran", 30, true)).andExpect(status().isOk());
        part(ticketId, screenId, 1)
                .andExpect(jsonPath("$.partsTotal").value(120.0))
                .andExpect(jsonPath("$.totalCost").value(0.0))
                .andExpect(jsonPath("$.paymentStatus").value("PAID"));
        moveTo(ticketId, "READY");

        postAs(SELLER, "/api/after-sales/%d/deliver".formatted(ticketId), "{}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));
    }

    @Test
    void warranty_cannotBeClaimedForADeviceNotSoldByTheShop() throws Exception {
        long ticketId = id(postAs(SELLER, "/api/after-sales", ticketJson(foreignImei)));
        moveTo(ticketId, "DIAGNOSING");

        putAs(TECHNICIAN, "/api/after-sales/" + ticketId, workJson("Ecran", 30, true))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("aucune garantie")));
    }

    // ---------- pieces et stock ----------

    @Test
    void removingAPart_returnsItToStock() throws Exception {
        long ticketId = id(postAs(SELLER, "/api/after-sales", ticketJson(foreignImei)));
        moveTo(ticketId, "DIAGNOSING");
        String body = part(ticketId, screenId, 2).andReturn().getResponse().getContentAsString();
        long partId = ((Number) JsonPath.read(body, "$.parts[0].id")).longValue();

        mockMvc.perform(delete("/api/after-sales/{id}/parts/{partId}", ticketId, partId)
                        .header(HttpHeaders.AUTHORIZATION, tokenFor(TECHNICIAN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parts.length()").value(0))
                .andExpect(jsonPath("$.partsTotal").value(0.0));

        getAs(MANAGER, "/api/products/" + screenId).andExpect(jsonPath("$.quantity").value(5));
        getAs(MANAGER, "/api/stock/movements?type=RETURN&q=" + ticketNumber(ticketId))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].quantityChange").value(2));
    }

    @Test
    void cancellation_returnsAllPartsToStock() throws Exception {
        long ticketId = id(postAs(SELLER, "/api/after-sales", ticketJson(foreignImei)));
        moveTo(ticketId, "DIAGNOSING");
        part(ticketId, screenId, 3);

        postAs(MANAGER, "/api/after-sales/%d/cancel".formatted(ticketId), "{\"reason\":\"Client renonce\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.statusHistory[2].comment").value("Client renonce"));

        getAs(MANAGER, "/api/products/" + screenId).andExpect(jsonPath("$.quantity").value(5));
    }

    @Test
    void insufficientStock_rejectsThePart_andLeavesTicketUnchanged() throws Exception {
        long ticketId = id(postAs(SELLER, "/api/after-sales", ticketJson(foreignImei)));
        moveTo(ticketId, "DIAGNOSING");

        part(ticketId, screenId, 6).andExpect(status().isConflict());

        getAs(TECHNICIAN, "/api/after-sales/" + ticketId).andExpect(jsonPath("$.parts.length()").value(0));
        getAs(MANAGER, "/api/products/" + screenId).andExpect(jsonPath("$.quantity").value(5));
    }

    @Test
    void failedDelivery_doesNotKeepThePaymentsItCarried() throws Exception {
        long ticketId = id(postAs(SELLER, "/api/after-sales", ticketJson(foreignImei)));
        moveTo(ticketId, "DIAGNOSING");
        putAs(TECHNICIAN, "/api/after-sales/" + ticketId, workJson("Batterie", 80, false));

        // Paiement valide, mais le ticket n'est pas pret : tout est annule
        postAs(SELLER, "/api/after-sales/%d/deliver".formatted(ticketId),
                "{\"payments\":[{\"amount\":80,\"method\":\"CASH\"}]}")
                .andExpect(status().isConflict());

        getAs(SELLER, "/api/after-sales/" + ticketId)
                .andExpect(jsonPath("$.payments.length()").value(0))
                .andExpect(jsonPath("$.amountDue").value(80.0));
    }

    @Test
    void invalidTransition_isRejected() throws Exception {
        long ticketId = id(postAs(SELLER, "/api/after-sales", ticketJson(foreignImei)));

        moveTo(ticketId, "READY")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("Transition impossible")));
        moveTo(ticketId, "DIAGNOSING");
        moveTo(ticketId, "IN_REPAIR")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("diagnostic")));
    }

    @Test
    void concurrentPayments_neverOverpayATicket() throws Exception {
        long ticketId = id(postAs(SELLER, "/api/after-sales", ticketJson(foreignImei)));
        moveTo(ticketId, "DIAGNOSING");
        putAs(TECHNICIAN, "/api/after-sales/" + ticketId, workJson("Batterie", 100, false)).andExpect(status().isOk());
        long sellerId = jdbcTemplate.queryForObject("select id from app_user where username = 'it-seller'", Long.class);
        int attempts = 8;
        AtomicInteger succeeded = new AtomicInteger();
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            tasks.add(() -> {
                Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject("it-seller").claim("uid", sellerId).build();
                SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
                try {
                    // Chaque tentative veut solder les 100 DT
                    afterSalesService.receivePayment(ticketId,
                            new PaymentRequest(new BigDecimal("100"), PaymentMethod.CASH, null));
                    succeeded.incrementAndGet();
                } catch (BusinessException expected) {
                    // deja solde par une autre tentative
                } finally {
                    SecurityContextHolder.clearContext();
                }
                return null;
            });
        }
        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        try {
            for (Future<Void> future : executor.invokeAll(tasks)) {
                future.get();
            }
        } finally {
            executor.shutdown();
        }

        assertThat(succeeded).hasValue(1);
        BigDecimal paid = jdbcTemplate.queryForObject(
                "select sum(amount) from after_sales_payment where ticket_id = ?", BigDecimal.class, ticketId);
        assertThat(paid).isEqualByComparingTo("100");
    }

    // ---------- recherche ----------

    @Test
    void search_filtersByImeiCustomerTechnicianAndOpenTickets() throws Exception {
        long first = id(postAs(SELLER, "/api/after-sales", ticketJson(foreignImei)));
        id(postAs(SELLER, "/api/after-sales", ticketJson(soldImei)));
        moveTo(first, "DIAGNOSING");
        postAs(MANAGER, "/api/after-sales/%d/cancel".formatted(first), "{\"reason\":\"Doublon\"}");
        long technicianId = jdbcTemplate.queryForObject("select id from app_user where username = 'it-technician'",
                Long.class);

        getAs(SELLER, "/api/after-sales?q=" + soldImei).andExpect(jsonPath("$.totalElements").value(1));
        getAs(SELLER, "/api/after-sales?q=ben salah").andExpect(jsonPath("$.totalElements").value(2));
        getAs(SELLER, "/api/after-sales?open=true")
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].imei").value(soldImei));
        getAs(TECHNICIAN, "/api/after-sales?technicianId=" + technicianId)
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("CANCELLED"));
        getAs(SELLER, "/api/after-sales?customerId=%d&status=RECEIVED".formatted(customerId))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ---------- validation et permissions ----------

    @Test
    void invalidReception_isRejected() throws Exception {
        postAs(SELLER, "/api/after-sales", ticketJson("123456789012345")).andExpect(status().isBadRequest());
        postAs(SELLER, "/api/after-sales", """
                {"customerId":%d,"deviceDescription":" ","reportedIssue":"x"}""".formatted(customerId))
                .andExpect(status().isBadRequest());
        postAs(SELLER, "/api/after-sales", """
                {"customerId":999,"deviceDescription":"Tel","reportedIssue":"x"}""")
                .andExpect(status().isNotFound());
    }

    @Test
    void permissions_followTheRoleMatrix() throws Exception {
        long ticketId = id(postAs(SELLER, "/api/after-sales", ticketJson(foreignImei)));

        mockMvc.perform(get("/api/after-sales")).andExpect(status().isUnauthorized());
        postAs(TECHNICIAN, "/api/after-sales", ticketJson(foreignImei)).andExpect(status().isForbidden());
        postAs(SELLER, "/api/after-sales/%d/status".formatted(ticketId), "{\"status\":\"DIAGNOSING\"}")
                .andExpect(status().isForbidden());
        putAs(SELLER, "/api/after-sales/" + ticketId, workJson("x", 1, false)).andExpect(status().isForbidden());
        postAs(SELLER, "/api/after-sales/%d/parts".formatted(ticketId),
                "{\"productId\":%d,\"quantity\":1}".formatted(screenId)).andExpect(status().isForbidden());
        postAs(TECHNICIAN, "/api/after-sales/%d/payments".formatted(ticketId), "{\"amount\":1,\"method\":\"CASH\"}")
                .andExpect(status().isForbidden());
        postAs(TECHNICIAN, "/api/after-sales/%d/deliver".formatted(ticketId), "{}").andExpect(status().isForbidden());
        postAs(TECHNICIAN, "/api/after-sales/%d/cancel".formatted(ticketId), "{\"reason\":\"x\"}")
                .andExpect(status().isForbidden());

        long technicianId = jdbcTemplate.queryForObject("select id from app_user where username = 'it-technician'",
                Long.class);
        patchAs(TECHNICIAN, "/api/after-sales/%d/technician".formatted(ticketId),
                "{\"technicianId\":%d}".formatted(technicianId)).andExpect(status().isForbidden());

        getAs(SELLER, "/api/after-sales/technicians")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.username=='it-technician')]").exists())
                .andExpect(jsonPath("$[?(@.username=='it-seller')]").doesNotExist());
        patchAs(MANAGER, "/api/after-sales/%d/technician".formatted(ticketId),
                "{\"technicianId\":%d}".formatted(technicianId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.technician.id").value(technicianId));
        long sellerId = jdbcTemplate.queryForObject("select id from app_user where username = 'it-seller'", Long.class);
        patchAs(MANAGER, "/api/after-sales/%d/technician".formatted(ticketId),
                "{\"technicianId\":%d}".formatted(sellerId)).andExpect(status().isBadRequest());
    }

    // ---------- outils ----------

    private ResultActions moveTo(long ticketId, String status) throws Exception {
        return postAs(TECHNICIAN, "/api/after-sales/%d/status".formatted(ticketId),
                "{\"status\":\"%s\"}".formatted(status));
    }

    private ResultActions part(long ticketId, long productId, int quantity) throws Exception {
        return postAs(TECHNICIAN, "/api/after-sales/%d/parts".formatted(ticketId),
                "{\"productId\":%d,\"quantity\":%d}".formatted(productId, quantity));
    }

    private String ticketJson(String imei) {
        return """
                {"customerId":%d,"deviceDescription":"Samsung A55 noir","imei":"%s",
                 "reportedIssue":"Ecran fissure, tactile inactif","estimatedCost":150}
                """.formatted(customerId, imei);
    }

    private static String workJson(String diagnosis, int laborCost, boolean underWarranty) {
        return """
                {"diagnosis":"%s","laborCost":%d,"underWarranty":%s}
                """.formatted(diagnosis, laborCost, underWarranty);
    }

    private String ticketNumber(long ticketId) {
        return jdbcTemplate.queryForObject("select ticket_number from after_sales_ticket where id = ?", String.class,
                ticketId);
    }

    private long unitId(String imei) throws Exception {
        String body = getAs(SELLER, "/api/stock/units?q=" + imei).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.content[0].id")).longValue();
    }

    private static long id(ResultActions result) throws Exception {
        String body = result.andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private static String productJson(String sku, long categoryId, String salePrice, boolean serialTracked,
                                      Integer warrantyMonths) {
        return """
                {"sku":"%s","name":"Produit %s","categoryId":%d,"type":"OTHER","condition":"NEW",
                 "purchasePrice":1,"salePrice":%s,"minStock":1,"serialTracked":%s,"warrantyMonths":%s}
                """.formatted(sku, sku, categoryId, salePrice, serialTracked, warrantyMonths);
    }
}
