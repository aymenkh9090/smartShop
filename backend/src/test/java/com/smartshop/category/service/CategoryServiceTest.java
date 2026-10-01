package com.smartshop.category.service;

import com.smartshop.category.dto.CategoryRequest;
import com.smartshop.category.entity.Category;
import com.smartshop.category.mapper.CategoryMapper;
import com.smartshop.category.repository.CategoryRepository;
import com.smartshop.common.exception.BusinessException;
import com.smartshop.common.exception.ResourceNotFoundException;
import com.smartshop.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ProductRepository productRepository;

    private CategoryService service;

    @BeforeEach
    void setUp() {
        service = new CategoryService(categoryRepository, productRepository, Mappers.getMapper(CategoryMapper.class));
    }

    @Test
    void create_trimsNameAndRejectsDuplicates() {
        when(categoryRepository.existsByNameIgnoreCase("Cables")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CategoryRequest("  Cables ", null)))
                .isInstanceOf(BusinessException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void create_savesNewCategory() {
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.create(new CategoryRequest(" Cables ", "  ")).name()).isEqualTo("Cables");
    }

    @Test
    void delete_isRefusedWhenProductsUseTheCategory() {
        Category category = new Category();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.countByCategoryId(1L)).thenReturn(3L);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("3 produit(s)");
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void delete_removesUnusedCategory() {
        Category category = new Category();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.countByCategoryId(1L)).thenReturn(0L);

        service.delete(1L);

        verify(categoryRepository).delete(category);
    }

    @Test
    void getEntity_unknownId_throwsNotFound() {
        when(categoryRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getEntity(9L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
