package com.smartshop.user;

import com.jayway.jsonpath.JsonPath;
import com.smartshop.AbstractIntegrationTest;
import com.smartshop.user.dto.UserCreateRequest;
import com.smartshop.user.entity.Role;
import com.smartshop.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserManagementIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "Secret123!";

    @Autowired
    private UserService userService;

    private Long sellerId;

    @BeforeEach
    void setUp() {
        // CASCADE : les mouvements de stock crees par d'autres classes de test referencent des utilisateurs
        jdbcTemplate.execute("truncate table refresh_token, app_user restart identity cascade");
        sellerId = userService.create(new UserCreateRequest("sami", "sami@smartshop.test", PASSWORD,
                "Sami", "Ben Ali", Role.SELLER)).id();
    }

    // ---------- acces ----------

    @Test
    void list_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
    }

    @Test
    void list_asManager_returns403() throws Exception {
        getAs(Role.MANAGER, "/api/users").andExpect(status().isForbidden());
    }

    @Test
    void create_asManagerWithInvalidBody_returns403BeforeValidation() throws Exception {
        postAs(Role.MANAGER, "/api/users", "{\"username\":\"x\"}").andExpect(status().isForbidden());
    }

    @Test
    void resetPassword_asSeller_returns403() throws Exception {
        putAs(Role.SELLER, "/api/users/" + sellerId + "/password",
                "{\"password\":\"Hacked123!\"}")
                .andExpect(status().isForbidden());
    }

    // ---------- CRUD ----------

    @Test
    void create_asAdmin_returns201WithLocation() throws Exception {
        postAs(Role.ADMIN, "/api/users", """
                {"username":"Karim.T","email":"Karim@Smartshop.test","password":"%s",
                 "firstName":"Karim","lastName":"Trabelsi","role":"TECHNICIAN"}
                """.formatted(PASSWORD))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/api/users/\\d+$")))
                .andExpect(jsonPath("$.username").value("karim.t"))
                .andExpect(jsonPath("$.email").value("karim@smartshop.test"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        login("karim.t", PASSWORD).andExpect(status().isOk());
    }

    @Test
    void create_withDuplicateUsername_returns409() throws Exception {
        postAs(Role.ADMIN, "/api/users", """
                {"username":"SAMI","email":"other@smartshop.test","password":"%s",
                 "firstName":"S","lastName":"B","role":"SELLER"}
                """.formatted(PASSWORD))
                .andExpect(status().isConflict());
    }

    @Test
    void search_filtersByTextAndRole() throws Exception {
        tokenFor(Role.ADMIN);
        getAs(Role.ADMIN, "/api/users?q=ben ali&role=SELLER")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].username").value("sami"));
    }

    @Test
    void update_changesProfileAndRole() throws Exception {
        putAs(Role.ADMIN, "/api/users/" + sellerId, """
                {"email":"sami.new@smartshop.test","firstName":"Sami","lastName":"Ben Ali","role":"MANAGER"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("sami.new@smartshop.test"))
                .andExpect(jsonPath("$.role").value("MANAGER"))
                .andExpect(jsonPath("$.username").value("sami"));
    }

    // ---------- statut / mot de passe ----------

    @Test
    void disable_blocksLoginAndRevokesRefreshTokens() throws Exception {
        String refreshToken = JsonPath.read(login("sami", PASSWORD).andReturn().getResponse().getContentAsString(),
                "$.refreshToken");

        patchAs(Role.ADMIN, "/api/users/" + sellerId + "/status", "{\"active\":false}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        login("sami", PASSWORD).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(refreshToken)))
                .andExpect(status().isUnauthorized());

        patchAs(Role.ADMIN, "/api/users/" + sellerId + "/status", "{\"active\":true}")
                .andExpect(status().isOk());
        login("sami", PASSWORD).andExpect(status().isOk());
    }

    @Test
    void disable_ownAccount_returns409() throws Exception {
        tokenFor(Role.ADMIN);
        Long adminId = jdbcTemplate.queryForObject("select id from app_user where username = 'it-admin'", Long.class);

        patchAs(Role.ADMIN, "/api/users/" + adminId + "/status", "{\"active\":false}")
                .andExpect(status().isConflict());
    }

    @Test
    void disable_lastActiveAdmin_returns409() throws Exception {
        tokenFor(Role.ADMIN);
        Long bossId = userService.create(new UserCreateRequest("boss", "boss@smartshop.test", PASSWORD,
                "Boss", "Admin", Role.ADMIN)).id();
        // Le compte appelant a ete desactive entre-temps (son JWT reste valide jusqu'a expiration)
        jdbcTemplate.update("update app_user set enabled = false where username = 'it-admin'");

        patchAs(Role.ADMIN, "/api/users/" + bossId + "/status", "{\"active\":false}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Impossible : il doit rester au moins un administrateur actif"));
        putAs(Role.ADMIN, "/api/users/" + bossId, """
                {"email":"boss@smartshop.test","firstName":"Boss","lastName":"Admin","role":"MANAGER"}
                """)
                .andExpect(status().isConflict());
    }

    @Test
    void resetPassword_replacesPassword() throws Exception {
        putAs(Role.ADMIN, "/api/users/" + sellerId + "/password", "{\"password\":\"NewSecret1!\"}")
                .andExpect(status().isOk());

        login("sami", PASSWORD).andExpect(status().isUnauthorized());
        login("sami", "NewSecret1!").andExpect(status().isOk());
    }

    @Test
    void resetPassword_tooShort_returns400() throws Exception {
        putAs(Role.ADMIN, "/api/users/" + sellerId + "/password", "{\"password\":\"short\"}")
                .andExpect(status().isBadRequest());
    }

    private ResultActions login(String username, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)));
    }
}
