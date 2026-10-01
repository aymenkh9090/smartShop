package com.smartshop.stock;

import com.jayway.jsonpath.JsonPath;
import com.smartshop.AbstractIntegrationTest;
import com.smartshop.TestImeis;
import com.smartshop.common.exception.InsufficientStockException;
import com.smartshop.stock.dto.StockMovementRequest;
import com.smartshop.stock.entity.StockMovementType;
import com.smartshop.stock.service.StockService;
import com.smartshop.user.entity.Role;
import com.smartshop.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StockIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private StockService stockService;
    @Autowired
    private UserRepository userRepository;

    private long categoryId;

    @BeforeEach
    void setUp() throws Exception {
        jdbcTemplate.execute("truncate table stock_movement_unit, stock_movement, product_unit, product, category "
                + "restart identity cascade");
        String body = postAs(MANAGER, "/api/categories", "{\"name\":\"Smartphones\"}")
                .andReturn().getResponse().getContentAsString();
        categoryId = ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    // ---------- produits simples ----------

    @Test
    void stockInAndOut_updateQuantityAndHistory() throws Exception {
        long productId = createProduct("CAB-1", false, 2);

        move(MANAGER, productId, "STOCK_IN", 10, null, null, null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantityChange").value(10))
                .andExpect(jsonPath("$.quantityAfter").value(10))
                .andExpect(jsonPath("$.createdBy.username").value("it-manager"));
        move(MANAGER, productId, "STOCK_OUT", 3, "Casse", null, null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantityChange").value(-3))
                .andExpect(jsonPath("$.quantityAfter").value(7));

        getAs(SELLER, "/api/products/" + productId).andExpect(jsonPath("$.quantity").value(7));
        getAs(SELLER, "/api/stock/movements?productId=" + productId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].quantityAfter", contains(7, 10)));
        getAs(TECHNICIAN, "/api/stock/movements?type=STOCK_OUT")
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void stockOut_beyondAvailable_returns409AndKeepsQuantity() throws Exception {
        long productId = createProduct("CAB-1", false, 2);
        move(MANAGER, productId, "STOCK_IN", 2, null, null, null);

        move(MANAGER, productId, "STOCK_OUT", 5, "Vente comptoir", null, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("Stock insuffisant")));

        getAs(SELLER, "/api/products/" + productId).andExpect(jsonPath("$.quantity").value(2));
        getAs(SELLER, "/api/stock/movements?productId=" + productId).andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void onlyManagers_canRecordMovements() throws Exception {
        long productId = createProduct("CAB-1", false, 2);

        move(SELLER, productId, "STOCK_IN", 1, null, null, null).andExpect(status().isForbidden());
        move(TECHNICIAN, productId, "STOCK_IN", 1, null, null, null).andExpect(status().isForbidden());
        postAs(SELLER, "/api/stock/inventory", "{\"lines\":[{\"productId\":%d,\"countedQuantity\":1}]}"
                .formatted(productId)).andExpect(status().isForbidden());
    }

    @Test
    void lowStockFilter_listsProductsAtOrBelowThreshold() throws Exception {
        long low = createProduct("LOW-1", false, 5);
        long ok = createProduct("OK-1", false, 1);
        move(MANAGER, low, "STOCK_IN", 5, null, null, null);
        move(MANAGER, ok, "STOCK_IN", 5, null, null, null);

        getAs(SELLER, "/api/products?lowStock=true")
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].sku").value("LOW-1"));
    }

    // ---------- produits suivis a l'unite ----------

    @Test
    void serializedProduct_tracksEachUnitByImei() throws Exception {
        long productId = createProduct("SM-A55", true, 1);
        String imei1 = TestImeis.valid(1);
        String imei2 = TestImeis.valid(2);

        move(MANAGER, productId, "STOCK_IN", 2, null,
                "[{\"imei\":\"%s\"},{\"imei\":\"%s\",\"serialNumber\":\"r58n\"}]".formatted(imei1, imei2), null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.units.length()").value(2))
                .andExpect(jsonPath("$.units[1].serialNumber").value("R58N"));

        String units = getAs(SELLER, "/api/stock/units?q=" + imei1)
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("IN_STOCK"))
                .andExpect(jsonPath("$.content[0].productSku").value("SM-A55"))
                .andReturn().getResponse().getContentAsString();
        long unitId = ((Number) JsonPath.read(units, "$.content[0].id")).longValue();

        move(MANAGER, productId, "STOCK_OUT", 1, "Ecran casse", null, "[" + unitId + "]")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.units[0].imei").value(imei1));

        getAs(SELLER, "/api/products/" + productId).andExpect(jsonPath("$.quantity").value(1));
        getAs(SELLER, "/api/stock/units?productId=%d&status=IN_STOCK".formatted(productId))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].imei").value(imei2));
        assertUnitsMatchQuantity(productId);
    }

    @Test
    void serializedProduct_rejectsInvalidAndDuplicateImeis() throws Exception {
        long productId = createProduct("SM-A55", true, 1);
        String imei = TestImeis.valid(1);

        move(MANAGER, productId, "STOCK_IN", 1, null, "[{\"imei\":\"490154203237519\"}]", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("units[0].imei"));

        move(MANAGER, productId, "STOCK_IN", 1, null, "[{\"imei\":\"%s\"}]".formatted(imei), null)
                .andExpect(status().isCreated());
        move(MANAGER, productId, "STOCK_IN", 1, null, "[{\"imei\":\"%s\"}]".formatted(imei), null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("IMEI deja enregistre(s) : " + imei));
        assertUnitsMatchQuantity(productId);
    }

    @Test
    void serialTracking_cannotChangeWhileInStock() throws Exception {
        long productId = createProduct("CAB-1", false, 1);
        move(MANAGER, productId, "STOCK_IN", 1, null, null, null);

        putAs(MANAGER, "/api/products/" + productId, productJson("CAB-1", true, 1))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("suivi a l'unite")));
    }

    // ---------- inventaire ----------

    @Test
    void inventory_createsAdjustmentsForDifferencesOnly() throws Exception {
        long first = createProduct("A-1", false, 0);
        long second = createProduct("B-1", false, 0);
        move(MANAGER, first, "STOCK_IN", 10, null, null, null);
        move(MANAGER, second, "STOCK_IN", 4, null, null, null);

        postAs(MANAGER, "/api/stock/inventory", """
                {"reference":"INV-2026-10","lines":[
                  {"productId":%d,"countedQuantity":8},{"productId":%d,"countedQuantity":4}]}
                """.formatted(first, second))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countedProducts").value(2))
                .andExpect(jsonPath("$.adjustedProducts").value(1))
                .andExpect(jsonPath("$.movements[0].quantityChange").value(-2))
                .andExpect(jsonPath("$.movements[0].reason").value("Inventaire"));

        getAs(SELLER, "/api/products/" + first).andExpect(jsonPath("$.quantity").value(8));
        getAs(SELLER, "/api/stock/movements?q=INV-2026").andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void inventory_isAllOrNothing() throws Exception {
        long simple = createProduct("A-1", false, 0);
        long serialized = createProduct("SM-1", true, 0);

        postAs(MANAGER, "/api/stock/inventory", """
                {"lines":[{"productId":%d,"countedQuantity":5},{"productId":%d,"countedQuantity":1}]}
                """.formatted(simple, serialized))
                .andExpect(status().isBadRequest());

        getAs(SELLER, "/api/products/" + simple).andExpect(jsonPath("$.quantity").value(0));
        getAs(SELLER, "/api/stock/movements").andExpect(jsonPath("$.totalElements").value(0));
    }

    // ---------- concurrence ----------

    @Test
    void concurrentStockOuts_neverOversell() throws Exception {
        long productId = createProduct("HOT-1", false, 0);
        move(MANAGER, productId, "STOCK_IN", 5, null, null, null);
        long managerId = userRepository.findByUsername("it-manager").orElseThrow().getId();

        int attempts = 12;
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger refused = new AtomicInteger();
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            tasks.add(() -> {
                authenticateAs(managerId);
                try {
                    stockService.recordMovement(new StockMovementRequest(productId, StockMovementType.STOCK_OUT, 1,
                            "Vente", null, null, null));
                    succeeded.incrementAndGet();
                } catch (InsufficientStockException e) {
                    refused.incrementAndGet();
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

        assertThat(succeeded).hasValue(5);
        assertThat(refused).hasValue(attempts - 5);
        Integer quantity = jdbcTemplate.queryForObject("select quantity from product where id = ?", Integer.class, productId);
        Integer lastAfter = jdbcTemplate.queryForObject(
                "select min(quantity_after) from stock_movement where product_id = ?", Integer.class, productId);
        assertThat(quantity).isZero();
        assertThat(lastAfter).isZero();
    }

    // ---------- helpers ----------

    private void authenticateAs(long userId) {
        Jwt jwt = Jwt.withTokenValue("test").header("alg", "none").subject("it-manager").claim("uid", userId).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    private void assertUnitsMatchQuantity(long productId) {
        Integer quantity = jdbcTemplate.queryForObject("select quantity from product where id = ?", Integer.class, productId);
        Integer inStock = jdbcTemplate.queryForObject(
                "select count(*) from product_unit where product_id = ? and status = 'IN_STOCK'", Integer.class, productId);
        assertThat(inStock).isEqualTo(quantity);
    }

    private long createProduct(String sku, boolean serialTracked, int minStock) throws Exception {
        String body = postAs(MANAGER, "/api/products", productJson(sku, serialTracked, minStock))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private String productJson(String sku, boolean serialTracked, int minStock) {
        return """
                {"sku":"%s","name":"Produit %s","categoryId":%d,"type":"OTHER","condition":"NEW",
                 "purchasePrice":10,"salePrice":15,"minStock":%d,"serialTracked":%s}
                """.formatted(sku, sku, categoryId, minStock, serialTracked);
    }

    private org.springframework.test.web.servlet.ResultActions move(Role role, long productId, String type, int quantity,
                                                                    String reason, String unitsJson, String unitIdsJson)
            throws Exception {
        String json = """
                {"productId":%d,"type":"%s","quantity":%d,"reason":%s,"units":%s,"unitIds":%s}
                """.formatted(productId, type, quantity, reason == null ? "null" : "\"" + reason + "\"",
                unitsJson == null ? "null" : unitsJson, unitIdsJson == null ? "null" : unitIdsJson);
        return postAs(role, "/api/stock/movements", json);
    }
}
