package com.smartshop.catalog;

import com.jayway.jsonpath.JsonPath;
import com.smartshop.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.smartshop.user.entity.Role.ADMIN;
import static com.smartshop.user.entity.Role.MANAGER;
import static com.smartshop.user.entity.Role.SELLER;
import static com.smartshop.user.entity.Role.TECHNICIAN;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CustomerSupplierIntegrationTest extends AbstractIntegrationTest {

    @BeforeEach
    void clean() {
        jdbcTemplate.execute("truncate table customer, supplier restart identity cascade");
    }

    // ---------- customers ----------

    @Test
    void seller_createsCustomer_withNormalizedContactData() throws Exception {
        postAs(SELLER, "/api/customers", customerJson("+216 22 333 444", "Ali.Ben@Mail.TN", "ab123456"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.phone").value("+21622333444"))
                .andExpect(jsonPath("$.email").value("ali.ben@mail.tn"))
                .andExpect(jsonPath("$.cin").value("AB123456"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void customerPhone_isUnique_evenWithDifferentSpacing() throws Exception {
        postAs(SELLER, "/api/customers", customerJson("22333444", null, null)).andExpect(status().isCreated());
        postAs(SELLER, "/api/customers", customerJson("22 333 444", null, null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Un client avec le telephone '22333444' existe deja"));
    }

    @Test
    void customer_invalidData_returns400() throws Exception {
        postAs(SELLER, "/api/customers",
                "{\"firstName\":\"\",\"lastName\":\"Ben\",\"phone\":\"abc\",\"email\":\"not-an-email\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(3));
    }

    @Test
    void technician_canReadButNotWriteCustomers() throws Exception {
        long id = createCustomer("22333444");
        getAs(TECHNICIAN, "/api/customers/" + id).andExpect(status().isOk());
        postAs(TECHNICIAN, "/api/customers", customerJson("55666777", null, null)).andExpect(status().isForbidden());
    }

    @Test
    void onlyManager_canDeactivateCustomer() throws Exception {
        long id = createCustomer("22333444");
        patchAs(SELLER, "/api/customers/" + id + "/status", "{\"active\":false}").andExpect(status().isForbidden());
        patchAs(MANAGER, "/api/customers/" + id + "/status", "{\"active\":false}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void customerSearch_matchesPhoneTypedWithSpacesAndName() throws Exception {
        createCustomer("22333444");
        createCustomer("98765432");

        getAs(SELLER, "/api/customers?q={q}", "22 333").andExpect(jsonPath("$.totalElements").value(1));
        getAs(SELLER, "/api/customers?q={q}", "BEN SALAH").andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void customerUpdate_keepsOwnPhone_andRejectsAnotherCustomersPhone() throws Exception {
        long first = createCustomer("22333444");
        createCustomer("98765432");

        putAs(SELLER, "/api/customers/" + first, customerJson("22333444", "new@mail.tn", null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@mail.tn"));
        putAs(SELLER, "/api/customers/" + first, customerJson("98 765 432", null, null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Un client avec le telephone '98765432' existe deja"));
    }

    @Test
    void unknownCustomer_returns404() throws Exception {
        getAs(SELLER, "/api/customers/999").andExpect(status().isNotFound());
    }

    // ---------- suppliers ----------

    @Test
    void suppliers_areForbiddenToSellerAndTechnician() throws Exception {
        getAs(SELLER, "/api/suppliers").andExpect(status().isForbidden());
        getAs(TECHNICIAN, "/api/suppliers").andExpect(status().isForbidden());
        postAs(SELLER, "/api/suppliers", supplierJson("TechDistrib", null)).andExpect(status().isForbidden());
    }

    @Test
    void manager_managesSuppliers() throws Exception {
        String body = postAs(MANAGER, "/api/suppliers", supplierJson("TechDistrib SARL", "1234567/a/m/000"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.taxId").value("1234567/A/M/000"))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(body, "$.id")).longValue();

        getAs(ADMIN, "/api/suppliers?q=techdistrib").andExpect(jsonPath("$.totalElements").value(1));
        patchAs(MANAGER, "/api/suppliers/" + id + "/status", "{\"active\":false}")
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void supplierNameAndTaxId_areUnique() throws Exception {
        postAs(MANAGER, "/api/suppliers", supplierJson("TechDistrib", "TAX-1")).andExpect(status().isCreated());
        postAs(MANAGER, "/api/suppliers", supplierJson("TECHDISTRIB", null)).andExpect(status().isConflict());
        postAs(MANAGER, "/api/suppliers", supplierJson("Autre", "tax-1")).andExpect(status().isConflict());
        long other = ((Number) JsonPath.read(postAs(MANAGER, "/api/suppliers", supplierJson("Autre", null))
                .andReturn().getResponse().getContentAsString(), "$.id")).longValue();
        putAs(MANAGER, "/api/suppliers/" + other, supplierJson("techdistrib", null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Un fournisseur avec le nom 'techdistrib' existe deja"));
    }

    // ---------- helpers ----------

    private long createCustomer(String phone) throws Exception {
        String body = postAs(SELLER, "/api/customers", customerJson(phone, null, null))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private static String customerJson(String phone, String email, String cin) {
        return """
                {"firstName":"Ali","lastName":"Ben Salah","phone":"%s","email":%s,"address":"Tunis","cin":%s}
                """.formatted(phone, quoted(email), quoted(cin)).strip();
    }

    private static String supplierJson(String name, String taxId) {
        return """
                {"name":"%s","phone":"71 000 000","email":"contact@tech.tn","taxId":%s}
                """.formatted(name, quoted(taxId)).strip();
    }

    private static String quoted(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }
}
