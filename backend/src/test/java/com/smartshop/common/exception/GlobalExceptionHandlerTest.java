package com.smartshop.common.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FakeController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void validationErrorsReturn400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/fake").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"price\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/fake"))
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("name", "price")));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/fake").contentType(MediaType.APPLICATION_JSON).content("{oops"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Corps de requete illisible ou mal forme"));
    }

    @Test
    void resourceNotFoundReturns404() throws Exception {
        mockMvc.perform(get("/fake/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Produit introuvable : 42"));
    }

    @Test
    void businessExceptionReturns409() throws Exception {
        mockMvc.perform(get("/fake/business"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Stock insuffisant"));
    }

    @Test
    void accessDeniedReturns403() throws Exception {
        mockMvc.perform(get("/fake/forbidden"))
                .andExpect(status().isForbidden());
    }

    @Test
    void typeMismatchReturns400() throws Exception {
        mockMvc.perform(get("/fake/id/abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unsupportedMethodKeepsItsStatus() throws Exception {
        mockMvc.perform(delete("/fake"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void unexpectedErrorReturns500WithoutInternalDetails() throws Exception {
        mockMvc.perform(get("/fake/crash"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Erreur interne du serveur"))
                .andExpect(content().string(not(containsString("secret interne"))));
    }

    record FakeRequest(@NotBlank String name, @Positive BigDecimal price) {
    }

    @RestController
    static class FakeController {

        @PostMapping("/fake")
        void create(@Valid @RequestBody FakeRequest request) {
        }

        @GetMapping("/fake/not-found")
        void notFound() {
            throw new ResourceNotFoundException("Produit", 42);
        }

        @GetMapping("/fake/business")
        void business() {
            throw new BusinessException("Stock insuffisant");
        }

        @GetMapping("/fake/forbidden")
        void forbidden() {
            throw new AccessDeniedException("non");
        }

        @GetMapping("/fake/id/{id}")
        void byId(@PathVariable Long id) {
        }

        @GetMapping("/fake/crash")
        void crash() {
            throw new IllegalStateException("secret interne");
        }
    }
}
