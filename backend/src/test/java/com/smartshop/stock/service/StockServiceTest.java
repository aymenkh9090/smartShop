package com.smartshop.stock.service;

import com.smartshop.TestImeis;
import com.smartshop.category.entity.Category;
import com.smartshop.common.exception.BusinessException;
import com.smartshop.common.exception.InsufficientStockException;
import com.smartshop.product.entity.Product;
import com.smartshop.product.repository.ProductRepository;
import com.smartshop.security.CurrentUserProvider;
import com.smartshop.stock.dto.InventoryRequest;
import com.smartshop.stock.dto.InventoryResponse;
import com.smartshop.stock.dto.ProductUnitRequest;
import com.smartshop.stock.dto.StockMovementRequest;
import com.smartshop.stock.dto.StockMovementResponse;
import com.smartshop.stock.entity.ProductUnit;
import com.smartshop.stock.entity.ProductUnitStatus;
import com.smartshop.stock.entity.StockMovement;
import com.smartshop.stock.entity.StockMovementType;
import com.smartshop.stock.mapper.StockMapper;
import com.smartshop.stock.repository.ProductUnitRepository;
import com.smartshop.stock.repository.StockMovementRepository;
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
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static com.smartshop.stock.entity.StockMovementType.ADJUSTMENT;
import static com.smartshop.stock.entity.StockMovementType.RETURN;
import static com.smartshop.stock.entity.StockMovementType.STOCK_IN;
import static com.smartshop.stock.entity.StockMovementType.STOCK_OUT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StockServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private StockMovementRepository movementRepository;
    @Mock
    private ProductUnitRepository unitRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;

    private StockService service;
    private User manager;

    @BeforeEach
    void setUp() {
        service = new StockService(productRepository, movementRepository, unitRepository, currentUserProvider,
                Mappers.getMapper(StockMapper.class));
        manager = new User("manager", "m@shop.tn", "hash", "M", "M", Role.MANAGER);
        ReflectionTestUtils.setField(manager, "id", 9L);
        when(currentUserProvider.currentUser()).thenReturn(manager);
        when(movementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(unitRepository.saveAll(anyIterable())).thenAnswer(invocation -> invocation.getArgument(0));
        when(unitRepository.findExistingImeis(anyCollection())).thenReturn(List.of());
        when(unitRepository.findExistingSerialNumbers(anyLong(), anyCollection())).thenReturn(List.of());
    }

    // ---------- produits simples ----------

    @Test
    void stockIn_increasesQuantityAndTracesMovement() {
        Product product = product(1L, 5, false);

        StockMovementResponse response = service.recordMovement(request(1L, STOCK_IN, 10, null));

        assertThat(product.getQuantity()).isEqualTo(15);
        assertThat(response.quantityChange()).isEqualTo(10);
        assertThat(response.quantityAfter()).isEqualTo(15);
        assertThat(response.createdBy().username()).isEqualTo("manager");
    }

    @Test
    void stockOut_isSignedNegatively() {
        Product product = product(1L, 5, false);

        StockMovementResponse response = service.recordMovement(request(1L, STOCK_OUT, 2, "Casse"));

        assertThat(product.getQuantity()).isEqualTo(3);
        assertThat(response.quantityChange()).isEqualTo(-2);
    }

    @Test
    void stockOut_beyondAvailable_isRejectedAndNothingIsSaved() {
        Product product = product(1L, 2, false);

        assertThatThrownBy(() -> service.recordMovement(request(1L, STOCK_OUT, 3, "Vol")))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("2 disponible(s), 3 demande(s)");
        assertThat(product.getQuantity()).isEqualTo(2);
        verify(movementRepository, never()).save(any());
    }

    @Test
    void reasonIsMandatoryExceptForStockIn() {
        product(1L, 5, false);

        assertThatThrownBy(() -> service.recordMovement(request(1L, STOCK_OUT, 1, "  ")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("motif");
    }

    @Test
    void quantityMustBePositiveForInOutAndReturn() {
        product(1L, 5, false);

        assertThatThrownBy(() -> service.recordMovement(request(1L, STOCK_IN, -1, null)))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.recordMovement(request(1L, RETURN, 0, "Retour")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void adjustment_isSignedAndMustNotBeZero() {
        Product product = product(1L, 5, false);

        service.recordMovement(request(1L, ADJUSTMENT, -2, "Ecart"));
        assertThat(product.getQuantity()).isEqualTo(3);

        assertThatThrownBy(() -> service.recordMovement(request(1L, ADJUSTMENT, 0, "Ecart")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void stockIn_onInactiveProduct_isRejected() {
        Product product = product(1L, 0, false);
        product.setActive(false);

        assertThatThrownBy(() -> service.recordMovement(request(1L, STOCK_IN, 1, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("desactive");
    }

    @Test
    void simpleProduct_refusesUnits() {
        product(1L, 0, false);
        StockMovementRequest request = new StockMovementRequest(1L, STOCK_IN, 1, null, null,
                List.of(new ProductUnitRequest(TestImeis.valid(1), null)), null);

        assertThatThrownBy(() -> service.recordMovement(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("pas suivi a l'unite");
    }

    // ---------- produits suivis a l'unite ----------

    @Test
    void serializedStockIn_createsOneUnitPerItem() {
        Product product = product(1L, 0, true);
        StockMovementRequest request = new StockMovementRequest(1L, STOCK_IN, 2, null, "BL-1",
                List.of(new ProductUnitRequest(TestImeis.valid(1), null), new ProductUnitRequest(null, " sn-2 ")), null);

        StockMovementResponse response = service.recordMovement(request);

        assertThat(product.getQuantity()).isEqualTo(2);
        assertThat(response.units()).extracting(StockMovementResponse.UnitSummary::serialNumber)
                .containsExactly(null, "SN-2");
    }

    @Test
    void serializedStockIn_requiresExactlyOneUnitPerItem() {
        product(1L, 0, true);
        StockMovementRequest request = new StockMovementRequest(1L, STOCK_IN, 2, null, null,
                List.of(new ProductUnitRequest(TestImeis.valid(1), null)), null);

        assertThatThrownBy(() -> service.recordMovement(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("2 unite(s) attendue(s)");
    }

    @Test
    void serializedStockIn_rejectsDuplicateAndKnownImeis() {
        product(1L, 0, true);
        String imei = TestImeis.valid(1);
        StockMovementRequest duplicate = new StockMovementRequest(1L, STOCK_IN, 2, null, null,
                List.of(new ProductUnitRequest(imei, null), new ProductUnitRequest(imei, null)), null);
        assertThatThrownBy(() -> service.recordMovement(duplicate)).hasMessageContaining("IMEI en double");

        when(unitRepository.findExistingImeis(anyCollection())).thenReturn(List.of(imei));
        StockMovementRequest known = new StockMovementRequest(1L, STOCK_IN, 1, null, null,
                List.of(new ProductUnitRequest(imei, null)), null);
        assertThatThrownBy(() -> service.recordMovement(known))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("deja enregistre");
    }

    @Test
    void serializedStockIn_requiresAnIdentifierPerUnit() {
        product(1L, 0, true);
        StockMovementRequest request = new StockMovementRequest(1L, STOCK_IN, 1, null, null,
                List.of(new ProductUnitRequest(" ", null)), null);

        assertThatThrownBy(() -> service.recordMovement(request)).hasMessageContaining("IMEI ou un numero de serie");
    }

    @Test
    void serializedStockOut_removesTheDesignatedUnits() {
        Product product = product(1L, 2, true);
        ProductUnit unit = unit(10L, product, ProductUnitStatus.IN_STOCK);
        when(unitRepository.findAllById(any())).thenReturn(List.of(unit));

        StockMovementResponse response = service.recordMovement(
                new StockMovementRequest(1L, STOCK_OUT, 1, "Casse", null, null, List.of(10L)));

        assertThat(unit.getStatus()).isEqualTo(ProductUnitStatus.REMOVED);
        assertThat(product.getQuantity()).isEqualTo(1);
        assertThat(response.units()).hasSize(1);
    }

    @Test
    void serializedStockOut_rejectsUnitsNotInStockOrFromAnotherProduct() {
        Product product = product(1L, 2, true);
        Product other = product(2L, 1, true);
        ProductUnit removed = unit(10L, product, ProductUnitStatus.REMOVED);
        ProductUnit foreign = unit(11L, other, ProductUnitStatus.IN_STOCK);

        when(unitRepository.findAllById(any())).thenReturn(List.of(removed));
        assertThatThrownBy(() -> service.recordMovement(
                new StockMovementRequest(1L, STOCK_OUT, 1, "Casse", null, null, List.of(10L))))
                .hasMessageContaining("n'est pas en stock");

        when(unitRepository.findAllById(any())).thenReturn(List.of(foreign));
        assertThatThrownBy(() -> service.recordMovement(
                new StockMovementRequest(1L, STOCK_OUT, 1, "Casse", null, null, List.of(11L))))
                .hasMessageContaining("n'appartient pas");
        assertThat(product.getQuantity()).isEqualTo(2);
    }

    @Test
    void serializedReturn_putsUnitsBackInStock() {
        Product product = product(1L, 0, true);
        ProductUnit unit = unit(10L, product, ProductUnitStatus.REMOVED);
        when(unitRepository.findAllById(any())).thenReturn(List.of(unit));

        service.recordMovement(new StockMovementRequest(1L, RETURN, 1, "Retour client", null, null, List.of(10L)));

        assertThat(unit.getStatus()).isEqualTo(ProductUnitStatus.IN_STOCK);
        assertThat(product.getQuantity()).isEqualTo(1);
    }

    // ---------- inventaire ----------

    @Test
    void inventory_adjustsOnlyDifferences_lockingInProductIdOrder() {
        Product first = product(1L, 10, false);
        Product second = product(2L, 4, false);
        Product third = product(3L, 7, false);

        InventoryResponse response = service.applyInventory(new InventoryRequest("INV-1", null, List.of(
                new InventoryRequest.Line(3L, 7), new InventoryRequest.Line(1L, 8), new InventoryRequest.Line(2L, 6))));

        assertThat(response.countedProducts()).isEqualTo(3);
        assertThat(response.adjustedProducts()).isEqualTo(2);
        assertThat(first.getQuantity()).isEqualTo(8);
        assertThat(second.getQuantity()).isEqualTo(6);
        assertThat(third.getQuantity()).isEqualTo(7);
        assertThat(response.movements()).allSatisfy(m -> {
            assertThat(m.type()).isEqualTo(StockMovementType.ADJUSTMENT);
            assertThat(m.reason()).isEqualTo("Inventaire");
            assertThat(m.reference()).isEqualTo("INV-1");
        });
        InOrder order = inOrder(productRepository);
        order.verify(productRepository).findByIdForUpdate(1L);
        order.verify(productRepository).findByIdForUpdate(2L);
        order.verify(productRepository).findByIdForUpdate(3L);
    }

    @Test
    void inventory_rejectsDuplicatesAndSerializedProducts() {
        product(1L, 1, false);
        product(2L, 1, true);

        assertThatThrownBy(() -> service.applyInventory(new InventoryRequest(null, null, List.of(
                new InventoryRequest.Line(1L, 1), new InventoryRequest.Line(1L, 2)))))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> service.applyInventory(new InventoryRequest(null, null, List.of(
                new InventoryRequest.Line(2L, 0)))))
                .hasMessageContaining("suivi a l'unite");
    }

    // ---------- helpers ----------

    private Product product(Long id, int quantity, boolean serialTracked) {
        Product product = new Product();
        ReflectionTestUtils.setField(product, "id", id);
        ReflectionTestUtils.setField(product, "quantity", quantity);
        product.setName("Produit " + id);
        product.setSku("SKU-" + id);
        product.setSerialTracked(serialTracked);
        product.setCategory(new Category());
        when(productRepository.findByIdForUpdate(id)).thenReturn(Optional.of(product));
        return product;
    }

    private static ProductUnit unit(Long id, Product product, ProductUnitStatus status) {
        ProductUnit unit = new ProductUnit(product, TestImeis.valid(id), null);
        ReflectionTestUtils.setField(unit, "id", id);
        unit.changeStatus(status);
        return unit;
    }

    private static StockMovementRequest request(Long productId, StockMovementType type, int quantity, String reason) {
        return new StockMovementRequest(productId, type, quantity, reason, null, null, null);
    }
}
