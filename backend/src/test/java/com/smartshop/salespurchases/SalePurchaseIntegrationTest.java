package com.smartshop.salespurchases;

import com.jayway.jsonpath.JsonPath;
import com.smartshop.AbstractIntegrationTest;
import com.smartshop.TestImeis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static com.smartshop.user.entity.Role.ADMIN;
import static com.smartshop.user.entity.Role.MANAGER;
import static com.smartshop.user.entity.Role.SELLER;
import static com.smartshop.user.entity.Role.TECHNICIAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SalePurchaseIntegrationTest extends AbstractIntegrationTest {

    private long supplierId;
    private long customerId;
    private long phoneId;
    private long cableId;
    private final String imei1 = TestImeis.valid(101);
    private final String imei2 = TestImeis.valid(102);

    @BeforeEach
    void setUp() throws Exception {
        jdbcTemplate.execute("""
                truncate table sale_payment, sale_item_unit, sale_item, sale, purchase_payment, purchase_item_unit,
                purchase_item, purchase, stock_movement_unit, stock_movement, product_unit, product, category,
                customer, supplier restart identity cascade""");
        long categoryId = id(postAs(MANAGER, "/api/categories", "{\"name\":\"Telephonie\"}"));
        supplierId = id(postAs(MANAGER, "/api/suppliers", "{\"name\":\"TechDistrib\"}"));
        customerId = id(postAs(SELLER, "/api/customers",
                "{\"firstName\":\"Ali\",\"lastName\":\"Ben Salah\",\"phone\":\"22333444\"}"));
        phoneId = id(postAs(MANAGER, "/api/products", productJson("SM-A55", categoryId, "1299.500", true, 12)));
        cableId = id(postAs(MANAGER, "/api/products", productJson("CAB-USB-C", categoryId, "19.990", false, null)));

        // Reception initiale : 2 telephones (IMEI) + 10 cables
        postAs(MANAGER, "/api/purchases", """
                {"supplierId":%d,"supplierInvoiceRef":"FAC-001","items":[
                  {"productId":%d,"quantity":2,"unitCost":1100,"units":[{"imei":"%s"},{"imei":"%s"}]},
                  {"productId":%d,"quantity":10,"unitCost":8.5}],
                 "payments":[{"amount":1000,"method":"BANK_TRANSFER"}],"dueDate":"%s"}
                """.formatted(supplierId, phoneId, imei1, imei2, cableId, LocalDate.now().plusDays(30)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reference").value(matchesPattern("A-\\d{4}-\\d{6}")))
                .andExpect(jsonPath("$.total").value(2285.0))
                .andExpect(jsonPath("$.amountDue").value(1285.0))
                .andExpect(jsonPath("$.paymentStatus").value("PARTIALLY_PAID"));
    }

    // ---------- achats ----------

    @Test
    void purchase_feedsStockAndUpdatesLastPurchasePrice() throws Exception {
        getAs(MANAGER, "/api/products/" + phoneId)
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.purchasePrice").value(1100.0));
        getAs(MANAGER, "/api/products/" + cableId).andExpect(jsonPath("$.quantity").value(10));
        getAs(MANAGER, "/api/stock/movements?q=FAC").andExpect(jsonPath("$.totalElements").value(0));
        getAs(MANAGER, "/api/purchases?supplierId=" + supplierId)
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].supplier.name").value("TechDistrib"));
    }

    @Test
    void purchases_areReservedToManagers() throws Exception {
        getAs(SELLER, "/api/purchases").andExpect(status().isForbidden());
        postAs(SELLER, "/api/purchases", "{\"supplierId\":%d,\"items\":[{\"productId\":%d,\"quantity\":1,\"unitCost\":1}]}"
                .formatted(supplierId, cableId)).andExpect(status().isForbidden());
    }

    // ---------- ventes ----------

    @Test
    void fullyPaidSale_updatesStockAndUnits() throws Exception {
        long unitId = unitId(imei1);

        postAs(SELLER, "/api/sales", """
                {"items":[{"productId":%d,"quantity":1,"unitIds":[%d]},{"productId":%d,"quantity":2}],
                 "discount":9.48,"payments":[{"amount":1000,"method":"CASH"},{"amount":330,"method":"CARD"}]}
                """.formatted(phoneId, unitId, cableId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.amountDue").value(0.0))
                .andExpect(jsonPath("$.items[?(@.productSku=='SM-A55')].warrantyMonths").value(12))
                .andExpect(jsonPath("$.items[?(@.productSku=='SM-A55')].units[0].imei").value(imei1))
                .andExpect(jsonPath("$.payments.length()").value(2));

        getAs(SELLER, "/api/products/" + phoneId).andExpect(jsonPath("$.quantity").value(1));
        getAs(SELLER, "/api/products/" + cableId).andExpect(jsonPath("$.quantity").value(8));
        getAs(SELLER, "/api/stock/units?q=" + imei1).andExpect(jsonPath("$.content[0].status").value("SOLD"));
        getAs(SELLER, "/api/stock/movements?type=STOCK_OUT")
                .andExpect(jsonPath("$.content[0].reason").value("Vente"))
                .andExpect(jsonPath("$.content[0].reference").value(matchesPattern("V-\\d{4}-\\d{6}")));
    }

    @Test
    void creditSaleWithoutCustomer_isRejected_andStockIsUntouched() throws Exception {
        postAs(SELLER, "/api/sales", """
                {"items":[{"productId":%d,"quantity":3}],"payments":[{"amount":10,"method":"CASH"}]}
                """.formatted(cableId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("necessite un client")));

        getAs(SELLER, "/api/products/" + cableId).andExpect(jsonPath("$.quantity").value(10));
        getAs(SELLER, "/api/stock/movements?type=STOCK_OUT").andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void creditSaleWithCustomer_keepsBalanceAndDueDate() throws Exception {
        LocalDate due = LocalDate.now().plusDays(15);
        postAs(SELLER, "/api/sales", """
                {"customerId":%d,"items":[{"productId":%d,"quantity":1,"unitIds":[%d]}],
                 "payments":[{"amount":300,"method":"CASH"}],"dueDate":"%s"}
                """.formatted(customerId, phoneId, unitId(imei2), due))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentStatus").value("PARTIALLY_PAID"))
                .andExpect(jsonPath("$.amountDue").value(999.5))
                .andExpect(jsonPath("$.dueDate").value(due.toString()))
                .andExpect(jsonPath("$.customer.lastName").value("Ben Salah"));

        getAs(SELLER, "/api/sales?customerId=" + customerId).andExpect(jsonPath("$.totalElements").value(1));
        getAs(SELLER, "/api/sales?paymentStatus=PARTIALLY_PAID&q=ben salah")
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void failureOnALaterLine_rollsBackEarlierStockMovements() throws Exception {
        assertThat(phoneId).as("le telephone doit etre traite avant le cable").isLessThan(cableId);
        long unitId = unitId(imei1);

        // Ligne 1 (telephone + IMEI) reussit, ligne 2 (cable) echoue : stock insuffisant
        postAs(SELLER, "/api/sales", """
                {"items":[{"productId":%d,"quantity":50},{"productId":%d,"quantity":1,"unitIds":[%d]}],
                 "payments":[{"amount":100,"method":"CASH"}]}
                """.formatted(cableId, phoneId, unitId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("Stock insuffisant")));

        // La sortie deja appliquee au telephone a ete annulee avec le reste de la transaction
        getAs(SELLER, "/api/products/" + phoneId).andExpect(jsonPath("$.quantity").value(2));
        getAs(SELLER, "/api/stock/units?q=" + imei1).andExpect(jsonPath("$.content[0].status").value("IN_STOCK"));
        getAs(SELLER, "/api/products/" + cableId).andExpect(jsonPath("$.quantity").value(10));
        getAs(SELLER, "/api/stock/movements?type=STOCK_OUT").andExpect(jsonPath("$.totalElements").value(0));
        getAs(SELLER, "/api/sales").andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void overpayment_isRejected() throws Exception {
        postAs(SELLER, "/api/sales", """
                {"items":[{"productId":%d,"quantity":1}],"payments":[{"amount":50,"method":"CASH"}]}
                """.formatted(cableId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("depasse le total")));
        getAs(SELLER, "/api/products/" + cableId).andExpect(jsonPath("$.quantity").value(10));
    }

    @Test
    void sellingAnInactiveProduct_isRejected() throws Exception {
        patchAs(MANAGER, "/api/products/" + cableId + "/status", "{\"active\":false}");

        postAs(SELLER, "/api/sales", """
                {"items":[{"productId":%d,"quantity":1}],"payments":[{"amount":19.99,"method":"CASH"}]}
                """.formatted(cableId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("desactive")));
    }

    @Test
    void cancellation_restoresStockAndImei_onlyForManagers() throws Exception {
        long saleId = id(postAs(SELLER, "/api/sales", """
                {"items":[{"productId":%d,"quantity":1,"unitIds":[%d]},{"productId":%d,"quantity":4}],
                 "payments":[{"amount":1379.46,"method":"CASH"}]}
                """.formatted(phoneId, unitId(imei1), cableId)));

        postAs(SELLER, "/api/sales/%d/cancel".formatted(saleId), "{\"reason\":\"Erreur\"}").andExpect(status().isForbidden());
        postAs(MANAGER, "/api/sales/%d/cancel".formatted(saleId), "{\"reason\":\"\"}").andExpect(status().isBadRequest());

        postAs(MANAGER, "/api/sales/%d/cancel".formatted(saleId), "{\"reason\":\"Client a change d'avis\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledBy").value("it-manager"))
                .andExpect(jsonPath("$.cancelReason").value("Client a change d'avis"));

        getAs(SELLER, "/api/products/" + phoneId).andExpect(jsonPath("$.quantity").value(2));
        getAs(SELLER, "/api/products/" + cableId).andExpect(jsonPath("$.quantity").value(10));
        getAs(SELLER, "/api/stock/units?q=" + imei1).andExpect(jsonPath("$.content[0].status").value("IN_STOCK"));
        getAs(SELLER, "/api/stock/movements?type=RETURN")
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].reason").value(containsString("Annulation vente")));

        postAs(ADMIN, "/api/sales/%d/cancel".formatted(saleId), "{\"reason\":\"Encore\"}").andExpect(status().isConflict());
    }

    @Test
    void technicians_cannotAccessSales() throws Exception {
        getAs(TECHNICIAN, "/api/sales").andExpect(status().isForbidden());
    }

    // ---------- helpers ----------

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
