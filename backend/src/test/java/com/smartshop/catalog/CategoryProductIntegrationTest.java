package com.smartshop.catalog;

import com.jayway.jsonpath.JsonPath;
import com.smartshop.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static com.smartshop.user.entity.Role.ADMIN;
import static com.smartshop.user.entity.Role.MANAGER;
import static com.smartshop.user.entity.Role.SELLER;
import static com.smartshop.user.entity.Role.TECHNICIAN;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CategoryProductIntegrationTest extends AbstractIntegrationTest {

    @BeforeEach
    void cleanCatalog() {
        jdbcTemplate.execute("truncate table product, category restart identity cascade");
    }

    // ---------- categories ----------

    @Test
    void manager_createsCategory_withLocationHeader() throws Exception {
        postAs(MANAGER, "/api/categories", "{\"name\":\"  Smartphones \",\"description\":\"Telephones\"}")
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, endsWith("/api/categories/1")))
                .andExpect(jsonPath("$.name").value("Smartphones"));
    }

    @Test
    void seller_cannotCreateCategory() throws Exception {
        postAs(SELLER, "/api/categories", "{\"name\":\"Cables\"}").andExpect(status().isForbidden());
    }

    @Test
    void categoryName_isUniqueIgnoringCase() throws Exception {
        long id = createCategory("Chargeurs");
        postAs(MANAGER, "/api/categories", "{\"name\":\"CHARGEURS\"}").andExpect(status().isConflict());

        long other = createCategory("Cables");
        putAs(MANAGER, "/api/categories/" + other, "{\"name\":\"chargeurs\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Une categorie nommee 'chargeurs' existe deja"));
        // Renommer une categorie avec son propre nom (casse differente) reste possible
        putAs(MANAGER, "/api/categories/" + id, "{\"name\":\"CHARGEURS\"}").andExpect(status().isOk());
    }

    @Test
    void category_inUse_cannotBeDeleted_butUnusedCanBe() throws Exception {
        long used = createCategory("Smartphones");
        createProduct(used, "SKU-1", null);
        long unused = createCategory("Divers");

        mockMvc.perform(delete("/api/categories/" + used).header(HttpHeaders.AUTHORIZATION, tokenFor(MANAGER)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Categorie utilisee par 1 produit(s) : impossible de la supprimer"));
        mockMvc.perform(delete("/api/categories/" + unused).header(HttpHeaders.AUTHORIZATION, tokenFor(MANAGER)))
                .andExpect(status().isNoContent());
        getAs(SELLER, "/api/categories/" + unused).andExpect(status().isNotFound());
    }

    // ---------- products ----------

    @Test
    void manager_createsProduct_normalizedWithZeroQuantity() throws Exception {
        long categoryId = createCategory("Smartphones");
        String json = productJson(categoryId, " sm-a55-128 ", "6901234567890")
                // La quantite envoyee doit etre ignoree : elle ne change que par mouvement de stock
                .replace("}", ",\"quantity\":50}");

        postAs(MANAGER, "/api/products", json)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("SM-A55-128"))
                .andExpect(jsonPath("$.quantity").value(0))
                .andExpect(jsonPath("$.lowStock").value(true))
                .andExpect(jsonPath("$.salePrice").value(1299.5))
                .andExpect(jsonPath("$.category.name").value("Smartphones"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void product_withUnknownCategory_returns404() throws Exception {
        postAs(MANAGER, "/api/products", productJson(999, "SKU-1", null)).andExpect(status().isNotFound());
    }

    @Test
    void product_skuAndBarcode_areUnique() throws Exception {
        long categoryId = createCategory("Smartphones");
        createProduct(categoryId, "SKU-1", "111");

        postAs(MANAGER, "/api/products", productJson(categoryId, "sku-1", null))
                .andExpect(status().isConflict());
        long other = createProduct(categoryId, "SKU-3", null);
        putAs(MANAGER, "/api/products/" + other, productJson(categoryId, "sku-1", null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Un produit avec le SKU 'SKU-1' existe deja"));
        postAs(MANAGER, "/api/products", productJson(categoryId, "SKU-2", "111"))
                .andExpect(status().isConflict());
    }

    @Test
    void product_withInvalidData_returns400() throws Exception {
        String json = """
                {"sku":"bad sku!","name":"","categoryId":null,"type":"SMARTPHONE","condition":"NEW",
                 "purchasePrice":-1,"salePrice":12.12345,"minStock":0}
                """;
        postAs(MANAGER, "/api/products", json)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field",
                        containsInAnyOrder("sku", "name", "categoryId", "purchasePrice", "salePrice", "serialTracked")));
    }

    @Test
    void product_writesAreForbiddenForSellerAndTechnician_readsAllowed() throws Exception {
        long categoryId = createCategory("Smartphones");
        long productId = createProduct(categoryId, "SKU-1", null);

        postAs(SELLER, "/api/products", productJson(categoryId, "SKU-2", null)).andExpect(status().isForbidden());
        putAs(TECHNICIAN, "/api/products/" + productId, productJson(categoryId, "SKU-1", null))
                .andExpect(status().isForbidden());
        patchAs(SELLER, "/api/products/" + productId + "/status", "{\"active\":false}")
                .andExpect(status().isForbidden());

        getAs(TECHNICIAN, "/api/products/" + productId).andExpect(status().isOk());
        getAs(SELLER, "/api/products").andExpect(status().isOk());
    }

    @Test
    void product_updateAndDeactivate() throws Exception {
        long categoryId = createCategory("Smartphones");
        long productId = createProduct(categoryId, "SKU-1", null);
        String updated = productJson(categoryId, "SKU-1", null).replace("Galaxy A55", "Galaxy A55 5G");

        putAs(MANAGER, "/api/products/" + productId, updated)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Galaxy A55 5G"));
        patchAs(ADMIN, "/api/products/" + productId + "/status", "{\"active\":false}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        getAs(SELLER, "/api/products?active=true").andExpect(jsonPath("$.totalElements").value(0));
        getAs(SELLER, "/api/products?active=false").andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void product_searchFilterAndPagination() throws Exception {
        long phones = createCategory("Smartphones");
        long cables = createCategory("Cables");
        createProduct(phones, "SKU-1", null);
        createProduct(phones, "SKU-2", null);
        long cableId = createProduct(cables, "CAB-1", null);
        putAs(MANAGER, "/api/products/" + cableId, productJson(cables, "CAB-1", null)
                .replace("Galaxy A55", "Cable USB-C 100%").replace("Samsung", "Anker"))
                .andExpect(status().isOk());

        getAs(SELLER, "/api/products?q=samsung").andExpect(jsonPath("$.totalElements").value(2));
        getAs(SELLER, "/api/products?categoryId=" + cables)
                .andExpect(jsonPath("$.content[0].sku").value("CAB-1"));
        getAs(SELLER, "/api/products?q={q}", "100%").andExpect(jsonPath("$.totalElements").value(1));
        // '%' est echappe : il ne joue pas le role de joker
        getAs(SELLER, "/api/products?q={q}", "A%B").andExpect(jsonPath("$.totalElements").value(0));
        getAs(SELLER, "/api/products?size=2&page=1&sort=sku,asc")
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[*].sku", hasItem("SKU-2")));
    }

    @Test
    void product_sortOnUnknownProperty_returns400() throws Exception {
        getAs(SELLER, "/api/products?sort=password")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Critere de tri invalide : password"));
    }

    @Test
    void product_unknownId_returns404() throws Exception {
        getAs(SELLER, "/api/products/12345").andExpect(status().isNotFound());
    }

    // ---------- helpers ----------

    private long createCategory(String name) throws Exception {
        String body = postAs(MANAGER, "/api/categories", "{\"name\":\"%s\"}".formatted(name))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long createProduct(long categoryId, String sku, String barcode) throws Exception {
        String body = postAs(MANAGER, "/api/products", productJson(categoryId, sku, barcode))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private static String productJson(long categoryId, String sku, String barcode) {
        return """
                {"sku":"%s","barcode":%s,"reference":"SM-A556B","name":"Galaxy A55","categoryId":%d,
                 "type":"SMARTPHONE","condition":"NEW","brand":"Samsung","model":"A55",
                 "purchasePrice":1100.000,"salePrice":1299.500,"minStock":2,"serialTracked":true,"warrantyMonths":12}
                """.formatted(sku, barcode == null ? "null" : "\"" + barcode + "\"", categoryId).strip();
    }
}
