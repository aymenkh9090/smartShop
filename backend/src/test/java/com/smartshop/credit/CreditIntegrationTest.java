package com.smartshop.credit;

import com.jayway.jsonpath.JsonPath;
import com.smartshop.AbstractIntegrationTest;
import com.smartshop.common.exception.BusinessException;
import com.smartshop.common.payment.PaymentMethod;
import com.smartshop.common.payment.PaymentRequest;
import com.smartshop.customercredit.service.CustomerCreditService;
import com.smartshop.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CreditIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CustomerCreditService customerCreditService;
    @Autowired
    private UserRepository userRepository;

    private long productId;
    private long supplierId;
    private long aliId;
    private long monaId;

    /** Une vente par statut de credit, plus une vente comptant (qui n'est pas un credit). */
    private long pendingSale;
    private long partialSale;
    private long overdueSale;
    private long paidSale;
    private long cancelledSale;
    private long cashSale;

    @BeforeEach
    void setUp() throws Exception {
        jdbcTemplate.execute("""
                truncate table sale_payment, sale_item_unit, sale_item, sale, purchase_payment, purchase_item_unit,
                purchase_item, purchase, stock_movement_unit, stock_movement, product_unit, product, category,
                customer, supplier restart identity cascade""");
        long categoryId = id(postAs(MANAGER, "/api/categories", "{\"name\":\"Accessoires\"}"));
        supplierId = id(postAs(MANAGER, "/api/suppliers", "{\"name\":\"TechDistrib\"}"));
        productId = id(postAs(MANAGER, "/api/products", """
                {"sku":"CHG-20W","name":"Chargeur 20W","categoryId":%d,"type":"CHARGER","condition":"NEW",
                 "purchasePrice":20,"salePrice":100,"minStock":1,"serialTracked":false}
                """.formatted(categoryId)));
        // Achat a credit : 50 chargeurs a 20 DT = 1000 DT, 400 payes
        id(postAs(MANAGER, "/api/purchases", """
                {"supplierId":%d,"items":[{"productId":%d,"quantity":50,"unitCost":20}],
                 "payments":[{"amount":400,"method":"CASH"}],"dueDate":"%s"}
                """.formatted(supplierId, productId, LocalDate.now().plusDays(30))));
        aliId = id(postAs(SELLER, "/api/customers", "{\"firstName\":\"Ali\",\"lastName\":\"Ben Salah\",\"phone\":\"22333444\"}"));
        monaId = id(postAs(SELLER, "/api/customers", "{\"firstName\":\"Mona\",\"lastName\":\"Trabelsi\",\"phone\":\"98765432\"}"));

        LocalDate inTenDays = LocalDate.now().plusDays(10);
        pendingSale = sale(aliId, 2, 0, inTenDays);       // 200 dus
        partialSale = sale(aliId, 3, 100, inTenDays);     // 200 dus
        overdueSale = sale(monaId, 6, 100, inTenDays);    // 500 dus, echeance passee (forcee ci-dessous)
        paidSale = sale(monaId, 1, 50, null);             // solde ensuite par un paiement ulterieur
        cancelledSale = sale(monaId, 1, 0, null);         // annulee
        cashSale = sale(null, 1, 100, null);              // comptant : pas un credit

        // L'API refuse une echeance passee : on simule un credit dont l'echeance est depassee
        jdbcTemplate.update("update sale set due_date = ? where id = ?", LocalDate.now().minusDays(3), overdueSale);
        postAs(SELLER, "/api/customer-credits/%d/payments".formatted(paidSale), payment(50)).andExpect(status().isOk());
        postAs(MANAGER, "/api/sales/%d/cancel".formatted(cancelledSale), "{\"reason\":\"Erreur\"}").andExpect(status().isOk());
    }

    // ---------- statut : coherence Java / SQL ----------

    @ParameterizedTest
    @CsvSource({"PENDING", "PARTIALLY_PAID", "OVERDUE", "PAID", "CANCELLED"})
    void eachStatusFilter_returnsExactlyTheMatchingCredit_withTheSameComputedStatus(String status) throws Exception {
        long expected = switch (status) {
            case "PENDING" -> pendingSale;
            case "PARTIALLY_PAID" -> partialSale;
            case "OVERDUE" -> overdueSale;
            case "PAID" -> paidSale;
            default -> cancelledSale;
        };
        getAs(SELLER, "/api/customer-credits?status=" + status)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].saleId").value(expected))
                .andExpect(jsonPath("$.content[0].status").value(status));
    }

    @Test
    void creditList_excludesCashSales_andOpenFilterKeepsOnlyUnsettledCredits() throws Exception {
        getAs(SELLER, "/api/customer-credits").andExpect(jsonPath("$.totalElements").value(5));
        getAs(SELLER, "/api/customer-credits?open=true").andExpect(jsonPath("$.totalElements").value(3));
        getAs(SELLER, "/api/customer-credits/" + cashSale).andExpect(status().isNotFound());
        getAs(SELLER, "/api/customer-credits?q=trabelsi&open=true")
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].amountDue").value(500.0));
    }

    // ---------- paiements ulterieurs ----------

    @Test
    void laterPayments_reduceTheBalance_andAreKeptInHistory() throws Exception {
        postAs(SELLER, "/api/customer-credits/%d/payments".formatted(pendingSale), payment(150))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amountDue").value(50.0))
                .andExpect(jsonPath("$.status").value("PARTIALLY_PAID"));
        postAs(SELLER, "/api/customer-credits/%d/payments".formatted(pendingSale), payment(50))
                .andExpect(jsonPath("$.amountDue").value(0.0))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.payments.length()").value(2))
                .andExpect(jsonPath("$.payments[0].createdBy").value("it-seller"));

        getAs(SELLER, "/api/sales/" + pendingSale).andExpect(jsonPath("$.paymentStatus").value("PAID"));
    }

    @Test
    void invalidPayments_areRejected() throws Exception {
        postAs(SELLER, "/api/customer-credits/%d/payments".formatted(partialSale), payment(200.001))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("depasse le reste du")));
        postAs(SELLER, "/api/customer-credits/%d/payments".formatted(paidSale), payment(1))
                .andExpect(status().isConflict());
        postAs(SELLER, "/api/customer-credits/%d/payments".formatted(cancelledSale), payment(1))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("annulee")));
        postAs(SELLER, "/api/customer-credits/%d/payments".formatted(cashSale), payment(1))
                .andExpect(status().isNotFound());
        postAs(SELLER, "/api/customer-credits/%d/payments".formatted(partialSale), "{\"amount\":0,\"method\":\"CASH\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void concurrentPayments_neverOverpayACredit() throws Exception {
        long sellerId = userRepository.findByUsername("it-seller").orElseThrow().getId();
        int attempts = 8;
        AtomicInteger succeeded = new AtomicInteger();
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            tasks.add(() -> {
                Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject("it-seller").claim("uid", sellerId).build();
                SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
                try {
                    // Chaque tentative veut solder les 200 DT restants
                    customerCreditService.receivePayment(pendingSale,
                            new PaymentRequest(new BigDecimal("200"), PaymentMethod.CASH, null));
                    succeeded.incrementAndGet();
                } catch (BusinessException expected) {
                    // credit deja solde par une autre tentative
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
        BigDecimal paid = jdbcTemplate.queryForObject("select sum(amount) from sale_payment where sale_id = ?",
                BigDecimal.class, pendingSale);
        assertThat(paid).isEqualByComparingTo("200");
    }

    // ---------- soldes ----------

    @Test
    void balances_sumOpenCreditsPerCustomer_largestFirst() throws Exception {
        getAs(SELLER, "/api/customer-credits/balances")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                // Mona : 500 en retard (la vente soldee et l'annulee ne comptent pas)
                .andExpect(jsonPath("$.content[0].lastName").value("Trabelsi"))
                .andExpect(jsonPath("$.content[0].outstanding").value(500.0))
                .andExpect(jsonPath("$.content[0].overdue").value(500.0))
                .andExpect(jsonPath("$.content[0].openCredits").value(1))
                // Ali : 200 + 200, rien en retard
                .andExpect(jsonPath("$.content[1].lastName").value("Ben Salah"))
                .andExpect(jsonPath("$.content[1].outstanding").value(400.0))
                .andExpect(jsonPath("$.content[1].overdue").value(0))
                .andExpect(jsonPath("$.content[1].openCredits").value(2))
                .andExpect(jsonPath("$.content[1].nextDueDate").value(LocalDate.now().plusDays(10).toString()));

        getAs(SELLER, "/api/customer-credits/balances?customerId=" + aliId)
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].customerId").value(aliId));
    }

    // ---------- echeances et permissions ----------

    @Test
    void reschedule_isReservedToManagers_andRefusesPastDates() throws Exception {
        String later = "{\"dueDate\":\"%s\"}".formatted(LocalDate.now().plusDays(60));
        patchAs(SELLER, "/api/customer-credits/%d/due-date".formatted(overdueSale), later).andExpect(status().isForbidden());
        patchAs(MANAGER, "/api/customer-credits/%d/due-date".formatted(overdueSale),
                "{\"dueDate\":\"%s\"}".formatted(LocalDate.now().minusDays(1))).andExpect(status().isBadRequest());

        patchAs(MANAGER, "/api/customer-credits/%d/due-date".formatted(overdueSale), later)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PARTIALLY_PAID"));
        patchAs(MANAGER, "/api/customer-credits/%d/due-date".formatted(paidSale), later).andExpect(status().isConflict());
    }

    @Test
    void techniciansCannotSeeCredits_andSellersCannotSeeSupplierCredits() throws Exception {
        getAs(TECHNICIAN, "/api/customer-credits").andExpect(status().isForbidden());
        getAs(SELLER, "/api/supplier-credits").andExpect(status().isForbidden());
    }

    // ---------- credits fournisseurs ----------

    @Test
    void supplierCredit_canBeSettledByManagers() throws Exception {
        String body = getAs(MANAGER, "/api/supplier-credits?open=true")
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].amountDue").value(600.0))
                .andExpect(jsonPath("$.content[0].status").value("PARTIALLY_PAID"))
                .andReturn().getResponse().getContentAsString();
        long purchaseId = ((Number) JsonPath.read(body, "$.content[0].purchaseId")).longValue();

        getAs(MANAGER, "/api/supplier-credits/balances")
                .andExpect(jsonPath("$.content[0].name").value("TechDistrib"))
                .andExpect(jsonPath("$.content[0].outstanding").value(600.0));

        postAs(MANAGER, "/api/supplier-credits/%d/payments".formatted(purchaseId),
                "{\"amount\":600,\"method\":\"BANK_TRANSFER\",\"reference\":\"VIR-77\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.payments[1].reference").value("VIR-77"));

        getAs(MANAGER, "/api/supplier-credits/balances").andExpect(jsonPath("$.totalElements").value(0));
        getAs(MANAGER, "/api/supplier-credits?status=PAID").andExpect(jsonPath("$.totalElements").value(1));
        getAs(MANAGER, "/api/supplier-credits?status=CANCELLED").andExpect(jsonPath("$.totalElements").value(0));
    }

    // ---------- helpers ----------

    private long sale(Long customerId, int quantity, double paid, LocalDate dueDate) throws Exception {
        String payments = paid > 0 ? "[{\"amount\":%s,\"method\":\"CASH\"}]".formatted(paid) : "[]";
        return id(postAs(SELLER, "/api/sales", """
                {"customerId":%s,"items":[{"productId":%d,"quantity":%d}],"payments":%s,"dueDate":%s}
                """.formatted(customerId, productId, quantity, payments,
                dueDate == null ? "null" : "\"" + dueDate + "\"")));
    }

    private static String payment(double amount) {
        return "{\"amount\":%s,\"method\":\"CASH\"}".formatted(amount);
    }

    private static long id(ResultActions result) throws Exception {
        String body = result.andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}
