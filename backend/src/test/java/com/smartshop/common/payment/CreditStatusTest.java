package com.smartshop.common.payment;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CreditStatusTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 1);

    @Test
    void cancelledWinsOverEverything() {
        assertThat(CreditStatus.of(true, PaymentStatus.PAID, TODAY.minusDays(9), TODAY)).isEqualTo(CreditStatus.CANCELLED);
    }

    @Test
    void paidIsNeverOverdue() {
        assertThat(CreditStatus.of(false, PaymentStatus.PAID, TODAY.minusDays(9), TODAY)).isEqualTo(CreditStatus.PAID);
    }

    @Test
    void overdueOnlyAfterTheDueDate() {
        assertThat(CreditStatus.of(false, PaymentStatus.PARTIALLY_PAID, TODAY.minusDays(1), TODAY))
                .isEqualTo(CreditStatus.OVERDUE);
        assertThat(CreditStatus.of(false, PaymentStatus.UNPAID, TODAY, TODAY))
                .as("le jour de l'echeance n'est pas encore un retard").isEqualTo(CreditStatus.PENDING);
    }

    @Test
    void pendingOrPartiallyPaidOtherwise() {
        assertThat(CreditStatus.of(false, PaymentStatus.UNPAID, null, TODAY)).isEqualTo(CreditStatus.PENDING);
        assertThat(CreditStatus.of(false, PaymentStatus.PARTIALLY_PAID, TODAY.plusDays(3), TODAY))
                .isEqualTo(CreditStatus.PARTIALLY_PAID);
    }
}
