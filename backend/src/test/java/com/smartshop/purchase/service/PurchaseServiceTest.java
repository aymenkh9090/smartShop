package com.smartshop.purchase.service;

import com.smartshop.common.exception.BusinessException;
import com.smartshop.common.payment.PaymentMethod;
import com.smartshop.common.payment.PaymentRequest;
import com.smartshop.common.payment.PaymentStatus;
import com.smartshop.product.entity.Product;
import com.smartshop.purchase.dto.PurchaseRequest;
import com.smartshop.purchase.dto.PurchaseResponse;
import com.smartshop.purchase.entity.Purchase;
import com.smartshop.purchase.mapper.PurchaseMapper;
import com.smartshop.purchase.repository.PurchaseRepository;
import com.smartshop.security.CurrentUserProvider;
import com.smartshop.stock.entity.StockMovement;
import com.smartshop.stock.entity.StockMovementType;
import com.smartshop.stock.service.StockService;
import com.smartshop.supplier.entity.Supplier;
import com.smartshop.supplier.service.SupplierService;
import com.smartshop.user.entity.Role;
import com.smartshop.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PurchaseServiceTest {

    @Mock
    private PurchaseRepository purchaseRepository;
    @Mock
    private SupplierService supplierService;
    @Mock
    private StockService stockService;
    @Mock
    private CurrentUserProvider currentUserProvider;

    private PurchaseService service;
    private final Map<Long, Product> products = new HashMap<>();

    @BeforeEach
    void setUp() {
        service = new PurchaseService(purchaseRepository, supplierService, stockService, currentUserProvider,
                Mappers.getMapper(PurchaseMapper.class), Clock.fixed(Instant.parse("2026-10-01T10:00:00Z"), ZoneOffset.UTC));
        User manager = new User("manager", "m@shop.tn", "hash", "M", "M", Role.MANAGER);
        when(currentUserProvider.currentUser()).thenReturn(manager);
        Supplier supplier = new Supplier();
        supplier.setName("TechDistrib");
        when(supplierService.getActiveEntity(1L)).thenReturn(supplier);
        when(purchaseRepository.nextReferenceNumber()).thenReturn(7L);
        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(stockService.recordPurchase(anyLong(), anyInt(), anyList(), anyString())).thenAnswer(invocation -> {
            Product product = products.computeIfAbsent(invocation.getArgument(0), PurchaseServiceTest::product);
            return new StockMovement(product, StockMovementType.STOCK_IN, invocation.getArgument(1), "Achat", "A",
                    manager, List.of());
        });
    }

    @Test
    void create_updatesLastPurchasePrice_andComputesSupplierCredit() {
        PurchaseRequest request = new PurchaseRequest(1L, " FAC-123 ", List.of(
                new PurchaseRequest.Item(4L, 10, new BigDecimal("12.5"), null),
                new PurchaseRequest.Item(2L, 2, new BigDecimal("1000"), null)),
                List.of(new PaymentRequest(new BigDecimal("1000"), PaymentMethod.BANK_TRANSFER, "VIR-1")),
                LocalDate.of(2026, 11, 1), null);

        PurchaseResponse response = service.create(request);

        assertThat(response.reference()).isEqualTo("A-2026-000007");
        assertThat(response.supplierInvoiceRef()).isEqualTo("FAC-123");
        assertThat(response.total()).isEqualByComparingTo("2125");
        assertThat(response.amountDue()).isEqualByComparingTo("1125");
        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.PARTIALLY_PAID);
        assertThat(response.dueDate()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(products.get(4L).getPurchasePrice()).isEqualByComparingTo("12.500");
        assertThat(products.get(2L).getPurchasePrice()).isEqualByComparingTo("1000");
    }

    @Test
    void create_rejectsOverpayment() {
        PurchaseRequest request = new PurchaseRequest(1L, null, List.of(
                new PurchaseRequest.Item(4L, 1, new BigDecimal("10"), null)),
                List.of(new PaymentRequest(new BigDecimal("11"), PaymentMethod.CASH, null)), null, null);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("dépasse le total");
    }

    private static Product product(Long id) {
        Product product = new Product();
        ReflectionTestUtils.setField(product, "id", id);
        product.setName("P" + id);
        product.setSku("SKU-" + id);
        product.setPurchasePrice(BigDecimal.ONE);
        return product;
    }
}
