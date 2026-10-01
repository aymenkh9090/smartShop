package com.smartshop.product.service;

import com.smartshop.category.entity.Category;
import com.smartshop.category.service.CategoryService;
import com.smartshop.common.exception.BusinessException;
import com.smartshop.common.exception.ResourceNotFoundException;
import com.smartshop.product.dto.ProductRequest;
import com.smartshop.product.dto.ProductResponse;
import com.smartshop.product.entity.Product;
import com.smartshop.product.entity.ProductCondition;
import com.smartshop.product.entity.ProductType;
import com.smartshop.product.mapper.ProductMapper;
import com.smartshop.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryService categoryService;

    private ProductService service;
    private Category category;

    @BeforeEach
    void setUp() {
        service = new ProductService(productRepository, categoryService, Mappers.getMapper(ProductMapper.class));
        category = new Category();
        category.setName("Smartphones");
        ReflectionTestUtils.setField(category, "id", 1L);
    }

    @Test
    void create_normalizesCodes_attachesCategory_andStartsWithZeroStock() {
        when(categoryService.getEntity(1L)).thenReturn(category);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = service.create(request(" sm-a55 ", " ean-123 "));

        assertThat(response.sku()).isEqualTo("SM-A55");
        assertThat(response.barcode()).isEqualTo("EAN-123");
        assertThat(response.category().name()).isEqualTo("Smartphones");
        assertThat(response.quantity()).isZero();
        assertThat(response.lowStock()).isTrue();
        assertThat(response.purchasePrice()).isEqualByComparingTo("1100");
        assertThat(response.active()).isTrue();
    }

    @Test
    void create_withDuplicateSku_isRejectedBeforeSaving() {
        when(productRepository.existsBySku("SM-A55")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("sm-a55", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("SKU 'SM-A55'");
        verify(productRepository, never()).save(any());
    }

    @Test
    void create_withDuplicateBarcode_isRejected() {
        when(productRepository.existsByBarcode("123")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("SM-A55", "123")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("code-barres");
    }

    @Test
    void create_withoutBarcode_skipsBarcodeCheck() {
        when(categoryService.getEntity(1L)).thenReturn(category);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(request("SM-A55", "  "));

        verify(productRepository, never()).existsByBarcode(any());
    }

    @Test
    void update_checksUniquenessAgainstOtherProductsOnly() {
        Product existing = Mappers.getMapper(ProductMapper.class).toEntity(request("SM-A55", null));
        when(productRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(productRepository.existsBySkuAndIdNot("SM-A55-V2", 5L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(5L, request("sm-a55-v2", null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void updateStatus_deactivatesProduct() {
        Product existing = Mappers.getMapper(ProductMapper.class).toEntity(request("SM-A55", null));
        existing.setCategory(category);
        when(productRepository.findById(5L)).thenReturn(Optional.of(existing));

        assertThat(service.updateStatus(5L, false).active()).isFalse();
    }

    @Test
    void findById_unknownProduct_throwsNotFound() {
        when(productRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(5L)).isInstanceOf(ResourceNotFoundException.class);
    }

    private static ProductRequest request(String sku, String barcode) {
        return new ProductRequest(sku, barcode, "SM-A556B", "Galaxy A55", null, 1L, ProductType.SMARTPHONE,
                ProductCondition.NEW, "Samsung", "A55", new BigDecimal("1100.000"), new BigDecimal("1299.500"),
                2, true, 12);
    }
}
