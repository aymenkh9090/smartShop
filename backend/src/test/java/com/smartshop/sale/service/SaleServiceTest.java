package com.smartshop.sale.service;

import com.smartshop.common.exception.BusinessException;
import com.smartshop.common.payment.PaymentMethod;
import com.smartshop.common.payment.PaymentRequest;
import com.smartshop.customer.service.CustomerService;
import com.smartshop.product.entity.Product;
import com.smartshop.sale.dto.SaleCancelRequest;
import com.smartshop.sale.dto.SaleRequest;
import com.smartshop.sale.dto.SaleResponse;
import com.smartshop.sale.entity.Sale;
import com.smartshop.sale.mapper.SaleMapper;
import com.smartshop.sale.repository.SaleRepository;
import com.smartshop.security.CurrentUserProvider;
import com.smartshop.stock.entity.ProductUnit;
import com.smartshop.stock.entity.StockMovement;
import com.smartshop.stock.entity.StockMovementType;
import com.smartshop.stock.service.StockService;
import com.smartshop.user.entity.Role;
import com.smartshop.user.entity.User;
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
class SaleServiceTest {

    @Mock
    private SaleRepository saleRepository;
    @Mock
    private CustomerService customerService;
    @Mock
    private StockService stockService;
    @Mock
    private CurrentUserProvider currentUserProvider;

    private SaleService service;
    private final User seller = new User("seller", "s@shop.tn", "hash", "S", "S", Role.SELLER);

    @BeforeEach
    void setUp() {
        service = new SaleService(saleRepository, customerService, stockService, currentUserProvider,
                Mappers.getMapper(SaleMapper.class), Clock.fixed(Instant.parse("2026-10-01T10:00:00Z"), ZoneOffset.UTC));
        when(currentUserProvider.currentUser()).thenReturn(seller);
        when(saleRepository.nextReferenceNumber()).thenReturn(42L);
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(stockService.recordSale(anyLong(), anyInt(), anyList(), anyString())).thenAnswer(invocation ->
                movement(invocation.getArgument(0), "100.000"));
    }

    @Test
    void create_recordsStockInProductIdOrder_andComputesTotals() {
        SaleRequest request = new SaleRequest(null, List.of(item(9L, 1), item(3L, 2)), new BigDecimal("10"),
                List.of(new PaymentRequest(new BigDecimal("290"), PaymentMethod.CASH, null)), null, null);

        SaleResponse response = service.create(request);

        InOrder order = inOrder(stockService);
        order.verify(stockService).recordSale(eq(3L), eq(2), anyList(), eq("V-2026-000042"));
        order.verify(stockService).recordSale(eq(9L), eq(1), anyList(), eq("V-2026-000042"));
        assertThat(response.reference()).isEqualTo("V-2026-000042");
        assertThat(response.total()).isEqualByComparingTo("290");
        assertThat(response.payments()).singleElement().satisfies(p -> assertThat(p.createdBy()).isEqualTo("seller"));
    }

    @Test
    void create_rejectsDuplicateProductLinesBeforeTouchingStock() {
        SaleRequest request = new SaleRequest(null, List.of(item(3L, 1), item(3L, 2)), null, null, null, null);

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(stockService);
    }

    @Test
    void cancel_restoresStockOfEveryLineWithItsUnits() {
        Sale sale = new Sale("V-2026-000001", null, seller, null);
        ProductUnit unit = new ProductUnit(product(5L, "100"), "490154203237518", null);
        ReflectionTestUtils.setField(unit, "id", 77L);
        sale.addItem(product(5L, "100"), 1, List.of(unit));
        sale.addItem(product(2L, "50"), 3, List.of());
        when(saleRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sale));

        SaleResponse response = service.cancel(1L, new SaleCancelRequest(" Erreur de saisie "));

        InOrder order = inOrder(stockService);
        order.verify(stockService).recordSaleCancellation(2L, 3, List.of(), "V-2026-000001", "Erreur de saisie");
        order.verify(stockService).recordSaleCancellation(5L, 1, List.of(77L), "V-2026-000001", "Erreur de saisie");
        assertThat(response.status().name()).isEqualTo("CANCELLED");
        assertThat(response.cancelledBy()).isEqualTo("seller");
    }

    @Test
    void cancel_ofCancelledSale_isRejectedWithoutTouchingStock() {
        Sale sale = new Sale("V-1", null, seller, null);
        sale.cancel(seller, Instant.now(), "deja");
        when(saleRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sale));

        assertThatThrownBy(() -> service.cancel(1L, new SaleCancelRequest("x"))).isInstanceOf(BusinessException.class);
        verify(stockService, never()).recordSaleCancellation(anyLong(), anyInt(), anyList(), anyString(), anyString());
    }

    private static SaleRequest.Item item(Long productId, int quantity) {
        return new SaleRequest.Item(productId, quantity, null);
    }

    private static Product product(Long id, String salePrice) {
        Product product = new Product();
        ReflectionTestUtils.setField(product, "id", id);
        product.setName("P" + id);
        product.setSku("SKU-" + id);
        product.setSalePrice(new BigDecimal(salePrice));
        return product;
    }

    private static StockMovement movement(Long productId, String salePrice) {
        return new StockMovement(product(productId, salePrice), StockMovementType.STOCK_OUT, -1, "Vente", "V",
                new User("u", "u@x.tn", "h", "U", "U", Role.SELLER), List.of());
    }
}
