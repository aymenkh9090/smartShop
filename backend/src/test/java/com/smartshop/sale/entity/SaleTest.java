package com.smartshop.sale.entity;

import com.smartshop.common.exception.BusinessException;
import com.smartshop.common.payment.PaymentMethod;
import com.smartshop.common.payment.PaymentStatus;
import com.smartshop.customer.entity.Customer;
import com.smartshop.product.entity.Product;
import com.smartshop.user.entity.Role;
import com.smartshop.user.entity.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SaleTest {

    private static final User SELLER = new User("seller", "s@shop.tn", "hash", "S", "S", Role.SELLER);
    private static final LocalDate DUE = LocalDate.of(2026, 11, 30);

    @Test
    void computesTotalsWithThreeDecimals() {
        Sale sale = new Sale("V-1", null, SELLER, null);
        sale.addItem(product("1299.500", 12), 2, List.of());
        sale.addItem(product("19.990", null), 3, List.of());
        sale.addPayment(new BigDecimal("2600.970"), PaymentMethod.CASH, null, SELLER, Instant.now());

        sale.complete(new BigDecimal("58.000"), DUE);

        assertThat(sale.getSubtotal()).isEqualByComparingTo("2658.970");
        assertThat(sale.getTotal()).isEqualByComparingTo("2600.970");
        assertThat(sale.getAmountDue()).isEqualByComparingTo("0");
        assertThat(sale.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(sale.getDueDate()).as("pas d'echeance sur une vente soldee").isNull();
        assertThat(sale.getItems().getFirst().getUnitPrice()).isEqualByComparingTo("1299.500");
        assertThat(sale.getItems().getFirst().getWarrantyMonths()).isEqualTo(12);
    }

    @Test
    void creditSale_requiresACustomer() {
        Sale anonymous = new Sale("V-1", null, SELLER, null);
        anonymous.addItem(product("100", null), 1, List.of());
        anonymous.addPayment(new BigDecimal("40"), PaymentMethod.CASH, null, SELLER, Instant.now());

        assertThatThrownBy(() -> anonymous.complete(null, DUE))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("necessite un client");
    }

    @Test
    void creditSale_withCustomer_keepsDueDateAndBalance() {
        Sale sale = new Sale("V-1", new Customer(), SELLER, null);
        sale.addItem(product("800", null), 1, List.of());
        sale.addPayment(new BigDecimal("300"), PaymentMethod.CASH, null, SELLER, Instant.now());

        sale.complete(null, DUE);

        assertThat(sale.getPaymentStatus()).isEqualTo(PaymentStatus.PARTIALLY_PAID);
        assertThat(sale.getAmountDue()).isEqualByComparingTo("500");
        assertThat(sale.getDueDate()).isEqualTo(DUE);
    }

    @Test
    void rejectsDiscountAboveSubtotalAndOverpayment() {
        Sale discounted = new Sale("V-1", null, SELLER, null);
        discounted.addItem(product("100", null), 1, List.of());
        assertThatThrownBy(() -> discounted.complete(new BigDecimal("100.001"), null)).hasMessageContaining("remise");

        Sale overpaid = new Sale("V-2", null, SELLER, null);
        overpaid.addItem(product("100", null), 1, List.of());
        overpaid.addPayment(new BigDecimal("150"), PaymentMethod.CASH, null, SELLER, Instant.now());
        assertThatThrownBy(() -> overpaid.complete(null, null)).hasMessageContaining("depasse le total");
    }

    @Test
    void unpaidSaleWithCustomer_isUnpaid() {
        Sale sale = new Sale("V-1", new Customer(), SELLER, null);
        sale.addItem(product("100", null), 1, List.of());

        sale.complete(null, null);

        assertThat(sale.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
    }

    @Test
    void cannotBeCancelledTwice() {
        Sale sale = new Sale("V-1", null, SELLER, null);
        sale.cancel(SELLER, Instant.now(), "Erreur");

        assertThat(sale.isCancelled()).isTrue();
        assertThatThrownBy(() -> sale.cancel(SELLER, Instant.now(), "Encore")).isInstanceOf(BusinessException.class);
    }

    private static Product product(String salePrice, Integer warrantyMonths) {
        Product product = new Product();
        product.setName("P");
        product.setSalePrice(new BigDecimal(salePrice));
        product.setWarrantyMonths(warrantyMonths);
        return product;
    }
}
