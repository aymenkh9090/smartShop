package com.smartshop.aftersales.service;

import com.smartshop.aftersales.dto.AfterSalesCancelRequest;
import com.smartshop.aftersales.dto.AfterSalesCreateRequest;
import com.smartshop.aftersales.dto.AfterSalesPartRequest;
import com.smartshop.aftersales.dto.AfterSalesResponse;
import com.smartshop.aftersales.dto.AfterSalesStatusRequest;
import com.smartshop.aftersales.dto.TechnicianAssignRequest;
import com.smartshop.aftersales.entity.AfterSalesStatus;
import com.smartshop.aftersales.entity.AfterSalesTicket;
import com.smartshop.aftersales.mapper.AfterSalesMapper;
import com.smartshop.aftersales.repository.AfterSalesTicketRepository;
import com.smartshop.common.exception.BusinessException;
import com.smartshop.config.BusinessCalendar;
import com.smartshop.customer.entity.Customer;
import com.smartshop.customer.service.CustomerService;
import com.smartshop.product.entity.Product;
import com.smartshop.product.repository.ProductRepository;
import com.smartshop.sale.entity.Sale;
import com.smartshop.sale.entity.SaleItem;
import com.smartshop.sale.repository.SaleRepository;
import com.smartshop.security.CurrentUserProvider;
import com.smartshop.stock.entity.ProductUnit;
import com.smartshop.stock.entity.StockMovement;
import com.smartshop.stock.entity.StockMovementType;
import com.smartshop.stock.repository.ProductUnitRepository;
import com.smartshop.stock.service.StockService;
import com.smartshop.user.entity.Role;
import com.smartshop.user.entity.User;
import com.smartshop.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AfterSalesServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-01T10:00:00Z");

    @Mock
    private AfterSalesTicketRepository ticketRepository;
    @Mock
    private CustomerService customerService;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductUnitRepository unitRepository;
    @Mock
    private SaleRepository saleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private StockService stockService;
    @Mock
    private CurrentUserProvider currentUserProvider;

    private AfterSalesService service;
    private final User seller = user("seller", Role.SELLER);
    private final User technician = user("tech", Role.TECHNICIAN);
    private final Customer customer = new Customer();

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new AfterSalesService(ticketRepository, customerService, productRepository, unitRepository,
                saleRepository, userRepository, stockService, currentUserProvider,
                Mappers.getMapper(AfterSalesMapper.class), new BusinessCalendar(clock, ZoneId.of("Africa/Tunis")), clock);
        customer.setFirstName("Ali");
        customer.setLastName("Ben Salah");
        when(customerService.getActiveEntity(1L)).thenReturn(customer);
        when(currentUserProvider.currentUser()).thenReturn(seller);
        when(ticketRepository.nextTicketNumber()).thenReturn(7L);
        when(ticketRepository.save(any(AfterSalesTicket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(unitRepository.findByImei(anyString())).thenReturn(Optional.empty());
        when(saleRepository.findCompletedItemsByUnit(anyLong())).thenReturn(List.of());
    }

    // ---------- reception ----------

    @Test
    void create_withUnknownDevice_hasNoWarranty() {
        AfterSalesResponse response = service.create(createRequest(" 490154203237518 ", null));

        assertThat(response.ticketNumber()).isEqualTo("SAV-2026-000007");
        assertThat(response.status()).isEqualTo(AfterSalesStatus.RECEIVED);
        assertThat(response.imei()).isEqualTo("490154203237518");
        assertThat(response.product()).isNull();
        assertThat(response.warrantyUntil()).isNull();
        assertThat(response.underWarranty()).isFalse();
        assertThat(response.createdBy()).isEqualTo("seller");
    }

    @Test
    void create_withUnitSoldByTheShop_linksProductAndSale_andAppliesWarranty() {
        Product phone = product(5L, "1299");
        phone.setWarrantyMonths(12);
        ProductUnit unit = unit(phone, 70L);
        when(unitRepository.findByImei("490154203237518")).thenReturn(Optional.of(unit));
        Sale sale = new Sale("V-2026-000001", customer, seller, null);
        ReflectionTestUtils.setField(sale, "createdAt", Instant.parse("2026-03-15T23:30:00Z")); // 16/03 a Tunis
        sale.addItem(phone, 1, List.of(unit));
        when(saleRepository.findCompletedItemsByUnit(70L)).thenReturn(List.copyOf(sale.getItems()));

        AfterSalesResponse response = service.create(createRequest("490154203237518", null));

        assertThat(response.product().id()).isEqualTo(5L);
        assertThat(response.unitId()).isEqualTo(70L);
        assertThat(response.saleReference()).isEqualTo("V-2026-000001");
        assertThat(response.warrantyUntil()).isEqualTo(LocalDate.of(2027, 3, 16));
        assertThat(response.underWarranty()).isTrue();
    }

    @Test
    void create_withProductNotMatchingTheSoldUnit_isRejected() {
        ProductUnit unit = unit(product(5L, "1299"), 70L);
        when(unitRepository.findByImei("490154203237518")).thenReturn(Optional.of(unit));
        when(productRepository.findById(9L)).thenReturn(Optional.of(product(9L, "10")));

        assertThatThrownBy(() -> service.create(createRequest("490154203237518", 9L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("correspond au produit");
        verify(ticketRepository, never()).save(any());
    }

    // ---------- travail technique ----------

    @Test
    void startingDiagnosis_assignsTheTechnicianWhenNoneIsAssigned() {
        when(currentUserProvider.currentUser()).thenReturn(technician);
        AfterSalesTicket ticket = ticket();
        when(ticketRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(ticket));

        AfterSalesResponse response = service.changeStatus(1L, new AfterSalesStatusRequest(AfterSalesStatus.DIAGNOSING, null));

        assertThat(response.status()).isEqualTo(AfterSalesStatus.DIAGNOSING);
        assertThat(response.technician().username()).isEqualTo("tech");
    }

    @Test
    void assignTechnician_rejectsAUserWhoIsNotATechnician() {
        ReflectionTestUtils.setField(seller, "id", 3L);
        when(userRepository.findById(3L)).thenReturn(Optional.of(seller));

        assertThatThrownBy(() -> service.assignTechnician(1L, new TechnicianAssignRequest(3L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("technicien");
        verify(ticketRepository, never()).findByIdForUpdate(anyLong());
    }

    @Test
    void addPart_onTicketOutsideTheWorkshop_neverTouchesStock() {
        when(ticketRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(ticket()));

        assertThatThrownBy(() -> service.addPart(1L, new AfterSalesPartRequest(4L, 1, null)))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(stockService);
    }

    @Test
    void addPart_takesThePartOutOfStockUnderTheTicketNumber() {
        AfterSalesTicket ticket = ticket();
        ticket.changeStatus(AfterSalesStatus.DIAGNOSING, null, technician, NOW);
        when(ticketRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(ticket));
        when(stockService.recordRepairPart(4L, 2, List.of(), "SAV-2026-000001"))
                .thenReturn(movement(product(4L, "35")));

        AfterSalesResponse response = service.addPart(1L, new AfterSalesPartRequest(4L, 2, null));

        assertThat(response.parts()).singleElement().satisfies(part -> {
            assertThat(part.productId()).isEqualTo(4L);
            assertThat(part.lineTotal()).isEqualByComparingTo("70");
        });
        assertThat(response.partsTotal()).isEqualByComparingTo("70");
    }

    // ---------- annulation ----------

    @Test
    void cancel_returnsEveryPartToStockInProductIdOrder() {
        AfterSalesTicket ticket = ticket();
        ticket.changeStatus(AfterSalesStatus.DIAGNOSING, null, technician, NOW);
        ProductUnit screen = unit(product(9L, "200"), 88L);
        ticket.addPart(product(9L, "200"), 1, List.of(screen), technician, NOW);
        ticket.addPart(product(2L, "5"), 3, List.of(), technician, NOW);
        when(ticketRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(ticket));

        AfterSalesResponse response = service.cancel(1L, new AfterSalesCancelRequest(" Client injoignable "));

        InOrder order = inOrder(stockService);
        order.verify(stockService).recordRepairPartReturn(eq(2L), eq(3), eq(List.of()), eq("SAV-2026-000001"),
                eq("annulation du ticket - Client injoignable"));
        order.verify(stockService).recordRepairPartReturn(eq(9L), eq(1), eq(List.of(88L)), eq("SAV-2026-000001"),
                anyString());
        assertThat(response.status()).isEqualTo(AfterSalesStatus.CANCELLED);
    }

    @Test
    void cancel_ofClosedTicket_isRejectedWithoutTouchingStock() {
        AfterSalesTicket ticket = ticket();
        ticket.cancel("deja", seller, NOW);
        when(ticketRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.cancel(1L, new AfterSalesCancelRequest("x")))
                .isInstanceOf(BusinessException.class);
        verify(stockService, never()).recordRepairPartReturn(anyLong(), anyInt(), anyList(), anyString(), anyString());
    }

    private AfterSalesTicket ticket() {
        return new AfterSalesTicket("SAV-2026-000001", customer,
                new AfterSalesTicket.Device("iPhone 12", null, null, null, null, null, null),
                "Ne charge plus", null, null, null, LocalDate.of(2026, 10, 1), seller, NOW);
    }

    private static AfterSalesCreateRequest createRequest(String imei, Long productId) {
        return new AfterSalesCreateRequest(1L, " iPhone 12 ", productId, imei, null, " Ne charge plus ",
                null, null, null);
    }

    private static User user(String username, Role role) {
        return new User(username, username + "@shop.tn", "h", "U", "U", role);
    }

    private static Product product(Long id, String salePrice) {
        Product product = new Product();
        ReflectionTestUtils.setField(product, "id", id);
        product.setName("P" + id);
        product.setSku("SKU-" + id);
        product.setSalePrice(new BigDecimal(salePrice));
        return product;
    }

    private static ProductUnit unit(Product product, Long id) {
        ProductUnit unit = new ProductUnit(product, "490154203237518", null);
        ReflectionTestUtils.setField(unit, "id", id);
        return unit;
    }

    private StockMovement movement(Product product) {
        return new StockMovement(product, StockMovementType.STOCK_OUT, -1, "Piece SAV", "SAV", technician, List.of());
    }
}
