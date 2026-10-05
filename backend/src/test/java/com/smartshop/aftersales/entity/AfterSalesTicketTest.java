package com.smartshop.aftersales.entity;

import com.smartshop.common.exception.BusinessException;
import com.smartshop.common.payment.PaymentMethod;
import com.smartshop.common.payment.PaymentStatus;
import com.smartshop.customer.entity.Customer;
import com.smartshop.product.entity.Product;
import com.smartshop.user.entity.Role;
import com.smartshop.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static com.smartshop.aftersales.entity.AfterSalesStatus.CANCELLED;
import static com.smartshop.aftersales.entity.AfterSalesStatus.DELIVERED;
import static com.smartshop.aftersales.entity.AfterSalesStatus.DIAGNOSING;
import static com.smartshop.aftersales.entity.AfterSalesStatus.IN_REPAIR;
import static com.smartshop.aftersales.entity.AfterSalesStatus.READY;
import static com.smartshop.aftersales.entity.AfterSalesStatus.RECEIVED;
import static com.smartshop.aftersales.entity.AfterSalesStatus.WAITING_FOR_PART;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AfterSalesTicketTest {

    private static final User TECH = new User("tech", "t@shop.tn", "h", "T", "T", Role.TECHNICIAN);
    private static final LocalDate RECEPTION = LocalDate.of(2026, 10, 1);
    private static final Instant NOW = Instant.parse("2026-10-01T10:00:00Z");

    @Test
    void newTicket_isReceived_withCreationInHistory_andNothingToPay() {
        AfterSalesTicket ticket = ticket(null);

        assertThat(ticket.getStatus()).isEqualTo(RECEIVED);
        assertThat(ticket.getStatusHistory()).singleElement().satisfies(change -> {
            assertThat(change.getFromStatus()).isNull();
            assertThat(change.getToStatus()).isEqualTo(RECEIVED);
        });
        assertThat(ticket.getTotalCost()).isEqualByComparingTo("0");
        assertThat(ticket.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(ticket.isUnderWarranty()).isFalse();
        assertThat(ticket.getNextStatuses()).containsExactly(DIAGNOSING);
    }

    @ParameterizedTest
    @CsvSource({"2026-10-01, true", "2026-09-30, false"})
    void warranty_isAppliedOnlyIfItCoversTheReceptionDate(LocalDate warrantyUntil, boolean covered) {
        assertThat(ticket(warrantyUntil).isUnderWarranty()).isEqualTo(covered);
    }

    @Test
    void workflow_followsAllowedTransitions_andRecordsEachChange() {
        AfterSalesTicket ticket = ticket(null);
        ticket.changeStatus(DIAGNOSING, null, TECH, NOW);
        diagnose(ticket, "0");
        ticket.changeStatus(WAITING_FOR_PART, "Ecran commande", TECH, NOW);
        ticket.changeStatus(IN_REPAIR, null, TECH, NOW);
        ticket.changeStatus(READY, null, TECH, NOW);

        assertThat(ticket.getStatusHistory()).extracting(AfterSalesStatusChange::getToStatus)
                .containsExactly(RECEIVED, DIAGNOSING, WAITING_FOR_PART, IN_REPAIR, READY);
        assertThat(ticket.getStatusHistory().get(2).getComment()).isEqualTo("Ecran commande");
        assertThat(ticket.getStatusHistory().get(2).getFromStatus()).isEqualTo(DIAGNOSING);
    }

    @ParameterizedTest
    @CsvSource({"READY", "IN_REPAIR", "DELIVERED", "CANCELLED"})
    void invalidTransition_fromReceived_isRejected(AfterSalesStatus target) {
        AfterSalesTicket ticket = ticket(null);

        assertThatThrownBy(() -> ticket.changeStatus(target, null, TECH, NOW))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Transition impossible");
        assertThat(ticket.getStatusHistory()).hasSize(1);
    }

    @Test
    void leavingDiagnosis_requiresADiagnosis() {
        AfterSalesTicket ticket = ticket(null);
        ticket.changeStatus(DIAGNOSING, null, TECH, NOW);

        assertThatThrownBy(() -> ticket.changeStatus(IN_REPAIR, null, TECH, NOW))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("diagnostic");
    }

    @Test
    void totalCost_isLaborPlusParts_orZeroUnderWarranty() {
        AfterSalesTicket ticket = ticket(RECEPTION.plusMonths(6));
        ticket.changeStatus(DIAGNOSING, null, TECH, NOW);
        ticket.addPart(product(1L, "45.500"), 2, List.of(), TECH, NOW);

        assertThat(ticket.getPartsTotal()).isEqualByComparingTo("91");
        assertThat(ticket.getTotalCost()).isEqualByComparingTo("0");

        diagnose(ticket, "30", false);
        assertThat(ticket.getTotalCost()).isEqualByComparingTo("121");
        assertThat(ticket.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
    }

    @Test
    void warranty_cannotBeAppliedToADeviceWithoutWarranty() {
        AfterSalesTicket ticket = ticket(null);

        assertThatThrownBy(() -> diagnose(ticket, "0", true))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void costChanges_cannotDropBelowWhatIsAlreadyPaid() {
        AfterSalesTicket ticket = ticket(null);
        diagnose(ticket, "50");
        ticket.receivePayment(new BigDecimal("40"), PaymentMethod.CASH, null, TECH, NOW);

        assertThat(ticket.getPaymentStatus()).isEqualTo(PaymentStatus.PARTIALLY_PAID);
        assertThatThrownBy(() -> diagnose(ticket, "30"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("déjà payé");
    }

    @Test
    void payment_aboveAmountDue_isRejected() {
        AfterSalesTicket ticket = ticket(null);
        diagnose(ticket, "50");

        assertThatThrownBy(() -> ticket.receivePayment(new BigDecimal("50.001"), PaymentMethod.CASH, null, TECH, NOW))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("dépasse le reste dû");
        assertThat(ticket.getPayments()).isEmpty();
    }

    @Test
    void parts_canOnlyChangeInTheWorkshop() {
        AfterSalesTicket ticket = ticket(null);

        assertThatThrownBy(() -> ticket.addPart(product(1L, "10"), 1, List.of(), TECH, NOW))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Pièces modifiables");
    }

    @Test
    void delivery_requiresReadyAndFullyPaidTicket() {
        AfterSalesTicket ticket = readyTicket("80");

        assertThatThrownBy(() -> ticket.deliver(null, TECH, NOW))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Reste à payer");

        ticket.receivePayment(new BigDecimal("80"), PaymentMethod.CARD, null, TECH, NOW);
        ticket.deliver("Remis au client", TECH, NOW);

        assertThat(ticket.getStatus()).isEqualTo(DELIVERED);
        assertThat(ticket.getDeliveredAt()).isEqualTo(NOW);
        assertThat(ticket.getNextStatuses()).isEmpty();
    }

    @Test
    void delivery_beforeReady_isRejected() {
        AfterSalesTicket ticket = ticket(null);

        assertThatThrownBy(() -> ticket.deliver(null, TECH, NOW))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("prêt");
    }

    @Test
    void closedTicket_rejectsAnyFurtherChange() {
        AfterSalesTicket ticket = ticket(null);
        ticket.cancel("Client injoignable", TECH, NOW);

        assertThat(ticket.getStatus()).isEqualTo(CANCELLED);
        assertThat(ticket.getStatusHistory().getLast().getComment()).isEqualTo("Client injoignable");
        assertThatThrownBy(() -> ticket.cancel("encore", TECH, NOW)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> ticket.changeStatus(DIAGNOSING, null, TECH, NOW)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> diagnose(ticket, "0")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> ticket.assignTechnician(TECH)).isInstanceOf(BusinessException.class);
    }

    private static AfterSalesTicket readyTicket(String laborCost) {
        AfterSalesTicket ticket = ticket(null);
        ticket.changeStatus(DIAGNOSING, null, TECH, NOW);
        diagnose(ticket, laborCost);
        ticket.changeStatus(IN_REPAIR, null, TECH, NOW);
        ticket.changeStatus(READY, null, TECH, NOW);
        return ticket;
    }

    private static void diagnose(AfterSalesTicket ticket, String laborCost) {
        diagnose(ticket, laborCost, ticket.isUnderWarranty());
    }

    private static void diagnose(AfterSalesTicket ticket, String laborCost, boolean underWarranty) {
        ticket.updateWork("Connecteur oxyde", null, new BigDecimal(laborCost), null, null, underWarranty, null,
                RECEPTION);
    }

    private static AfterSalesTicket ticket(LocalDate warrantyUntil) {
        Customer customer = new Customer();
        customer.setFirstName("Ali");
        customer.setLastName("Ben Salah");
        customer.setPhone("22333444");
        AfterSalesTicket.Device device = new AfterSalesTicket.Device("iPhone 12", null, null, null, null, null,
                warrantyUntil);
        return new AfterSalesTicket("SAV-2026-000001", customer, device, "Ne charge plus", null, null, null,
                RECEPTION, TECH, NOW);
    }

    private static Product product(Long id, String salePrice) {
        Product product = new Product();
        ReflectionTestUtils.setField(product, "id", id);
        product.setName("Pièce " + id);
        product.setSalePrice(new BigDecimal(salePrice));
        return product;
    }
}
