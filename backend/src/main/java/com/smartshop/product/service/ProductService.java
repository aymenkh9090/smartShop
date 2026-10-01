package com.smartshop.product.service;

import com.smartshop.category.entity.Category;
import com.smartshop.category.service.CategoryService;
import com.smartshop.common.dto.PageResponse;
import com.smartshop.common.exception.BusinessException;
import com.smartshop.common.exception.ResourceNotFoundException;
import com.smartshop.common.mapping.TextNormalizer;
import com.smartshop.product.dto.ProductFilter;
import com.smartshop.product.dto.ProductRequest;
import com.smartshop.product.dto.ProductResponse;
import com.smartshop.product.entity.Product;
import com.smartshop.product.mapper.ProductMapper;
import com.smartshop.product.repository.ProductRepository;
import com.smartshop.product.repository.ProductSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryService categoryService;
    private final ProductMapper productMapper;

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(ProductFilter filter, Pageable pageable) {
        return PageResponse.from(productRepository.findAll(ProductSpecifications.matching(filter), pageable)
                .map(productMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return productMapper.toResponse(getEntity(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = productMapper.toEntity(request);
        if (productRepository.existsBySku(product.getSku())) {
            throw duplicate("SKU", product.getSku());
        }
        if (product.getBarcode() != null && productRepository.existsByBarcode(product.getBarcode())) {
            throw duplicate("code-barres", product.getBarcode());
        }
        product.setCategory(categoryService.getEntity(request.categoryId()));
        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = getEntity(id);
        // Controles avant toute modification de l'entite managee (voir CategoryService#update)
        String sku = TextNormalizer.upper(request.sku());
        String barcode = TextNormalizer.upper(request.barcode());
        if (productRepository.existsBySkuAndIdNot(sku, id)) {
            throw duplicate("SKU", sku);
        }
        if (barcode != null && productRepository.existsByBarcodeAndIdNot(barcode, id)) {
            throw duplicate("code-barres", barcode);
        }
        Category category = categoryService.getEntity(request.categoryId());
        productMapper.update(request, product);
        product.setCategory(category);
        return productMapper.toResponse(product);
    }

    @Transactional
    public ProductResponse updateStatus(Long id, boolean active) {
        Product product = getEntity(id);
        product.setActive(active);
        return productMapper.toResponse(product);
    }

    private Product getEntity(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", id));
    }

    private static BusinessException duplicate(String field, String value) {
        return new BusinessException("Un produit avec le %s '%s' existe deja".formatted(field, value));
    }
}
