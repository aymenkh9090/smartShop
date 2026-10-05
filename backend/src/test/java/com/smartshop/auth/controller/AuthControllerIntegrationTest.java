package com.smartshop.auth.controller;

import com.jayway.jsonpath.JsonPath;
import com.smartshop.TestcontainersConfiguration;
import com.smartshop.user.dto.UserCreateRequest;
import com.smartshop.user.entity.Role;
import com.smartshop.user.entity.User;
import com.smartshop.user.repository.UserRepository;
import com.smartshop.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class AuthControllerIntegrationTest {

    private static final String PASSWORD = "Secret123!";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // CASCADE : les mouvements de stock crees par d'autres classes de test referencent des utilisateurs
        jdbcTemplate.execute("truncate table refresh_token, app_user restart identity cascade");
        createUser("admin", Role.ADMIN);
        createUser("seller", Role.SELLER);
    }

    // ---------- login ----------

    @Test
    void login_withUsername_returnsTokensAndUser() throws Exception {
        login("admin", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.user.username").value("admin"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    }

    @Test
    void login_withEmailIgnoringCase_succeeds() throws Exception {
        login("SELLER@smartshop.test", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("seller"));
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        login("admin", "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Identifiants invalides"));
    }

    @Test
    void login_withUnknownUser_returnsSame401() throws Exception {
        login("ghost", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Identifiants invalides"));
    }

    @Test
    void login_withDisabledAccount_returnsSame401() throws Exception {
        User seller = userRepository.findByUsername("seller").orElseThrow();
        seller.disable();
        userRepository.save(seller);

        login("seller", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Identifiants invalides"));
    }

    @Test
    void login_withMissingFields_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("username", "password")));
    }

    // ---------- me ----------

    @Test
    void me_withValidToken_returnsCurrentUser() throws Exception {
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(accessToken("seller"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("seller"))
                .andExpect(jsonPath("$.role").value("SELLER"));
    }

    @Test
    void me_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void me_withTamperedToken_returns401() throws Exception {
        String token = accessToken("seller");
        String tampered = token.substring(0, token.length() - 4) + "abcd";

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(tampered)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // ---------- register ----------

    @Test
    void register_asAdmin_createsUser() throws Exception {
        register(accessToken("admin"), newUserJson("tech1", "tech1@smartshop.test"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.username").value("tech1"))
                .andExpect(jsonPath("$.role").value("TECHNICIAN"))
                .andExpect(jsonPath("$.password").doesNotExist());

        login("tech1", PASSWORD).andExpect(status().isOk());
    }

    @Test
    void register_asSeller_returns403() throws Exception {
        register(accessToken("seller"), newUserJson("tech1", "tech1@smartshop.test"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void register_asSellerWithInvalidBody_returns403BeforeValidation() throws Exception {
        register(accessToken("seller"), "{\"username\":\"x\"}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    void register_asManager_returns403() throws Exception {
        createUser("manager", Role.MANAGER);
        register(accessToken("manager"), newUserJson("tech1", "tech1@smartshop.test"))
                .andExpect(status().isForbidden());
    }

    @Test
    void register_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(newUserJson("tech1", "tech1@smartshop.test")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_withDuplicateUsername_returns409() throws Exception {
        register(accessToken("admin"), newUserJson("SELLER", "other@smartshop.test"))
                .andExpect(status().isConflict());
    }

    @Test
    void register_withInvalidData_returns400() throws Exception {
        String json = """
                {"username":"x","email":"not-an-email","password":"short","firstName":"","lastName":"L","role":"SELLER"}
                """;
        register(accessToken("admin"), json)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field",
                        containsInAnyOrder("username", "email", "password", "firstName")));
    }

    // ---------- refresh / logout ----------

    @Test
    void refresh_rotatesTokenAndDetectsReuse() throws Exception {
        String firstRefresh = JsonPath.read(login("seller", PASSWORD).andReturn().getResponse().getContentAsString(),
                "$.refreshToken");

        String body = refresh(firstRefresh)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String secondRefresh = JsonPath.read(body, "$.refreshToken");

        // Le premier token a ete consomme : le rejouer revoque toute la session
        refresh(firstRefresh).andExpect(status().isUnauthorized());
        refresh(secondRefresh).andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_withUnknownToken_returns401() throws Exception {
        refresh("not-a-real-token")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Session expirée ou invalide, veuillez vous reconnecter"));
    }

    @Test
    void logout_revokesRefreshToken() throws Exception {
        String refreshToken = JsonPath.read(login("seller", PASSWORD).andReturn().getResponse().getContentAsString(),
                "$.refreshToken");

        mockMvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson(refreshToken)))
                .andExpect(status().isNoContent());

        refresh(refreshToken).andExpect(status().isUnauthorized());
    }

    // ---------- helpers ----------

    private void createUser(String username, Role role) {
        userService.create(new UserCreateRequest(username, username + "@smartshop.test", PASSWORD,
                "Prenom", "Nom", role));
    }

    private ResultActions login(String username, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)));
    }

    private String accessToken(String username) throws Exception {
        String body = login(username, PASSWORD).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private ResultActions register(String token, String json) throws Exception {
        return mockMvc.perform(post("/api/auth/register")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(refreshJson(refreshToken)));
    }

    private static String refreshJson(String refreshToken) {
        return "{\"refreshToken\":\"%s\"}".formatted(refreshToken);
    }

    private static String newUserJson(String username, String email) {
        return """
                {"username":"%s","email":"%s","password":"%s","firstName":"Karim","lastName":"Trabelsi","role":"TECHNICIAN"}
                """.formatted(username, email, PASSWORD);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
