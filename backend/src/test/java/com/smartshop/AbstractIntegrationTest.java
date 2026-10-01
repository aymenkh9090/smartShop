package com.smartshop;

import com.smartshop.security.JwtService;
import com.smartshop.user.entity.Role;
import com.smartshop.user.entity.User;
import com.smartshop.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Locale;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Base des tests d'integration HTTP : PostgreSQL reel (Testcontainers), contexte Spring partage,
 * et un JWT valide par role sans passer par /login.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected JdbcTemplate jdbcTemplate;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtService jwtService;

    protected String tokenFor(Role role) {
        String username = "it-" + role.name().toLowerCase(Locale.ROOT);
        User user = userRepository.findByUsername(username).orElseGet(() -> userRepository.save(
                new User(username, username + "@smartshop.test", "unused-hash", "IT", role.name(), role)));
        return "Bearer " + jwtService.generateAccessToken(user).value();
    }

    protected ResultActions getAs(Role role, String url, Object... uriVars) throws Exception {
        return mockMvc.perform(get(url, uriVars).header(HttpHeaders.AUTHORIZATION, tokenFor(role)));
    }

    protected ResultActions postAs(Role role, String url, String json) throws Exception {
        return mockMvc.perform(post(url).header(HttpHeaders.AUTHORIZATION, tokenFor(role))
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected ResultActions putAs(Role role, String url, String json) throws Exception {
        return mockMvc.perform(put(url).header(HttpHeaders.AUTHORIZATION, tokenFor(role))
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected ResultActions patchAs(Role role, String url, String json) throws Exception {
        return mockMvc.perform(patch(url).header(HttpHeaders.AUTHORIZATION, tokenFor(role))
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
