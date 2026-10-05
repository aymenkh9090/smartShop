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
import com.smartshop.product.entity.ProductCondition;
import com.smartshop.product.entity.ProductType;
import com.smartshop.product.mapper.ProductMapper;
import com.smartshop.product.repository.ProductRepository;
import com.smartshop.product.repository.ProductSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

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
        request = request.withDefaults(TextNormalizer.trimToNull(request.sku()) == null ? generateSku() : null,
                ProductCondition.NEW, BigDecimal.ZERO, 0, request.type() == ProductType.SMARTPHONE);
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
        request = request.withDefaults(product.getSku(), product.getCondition(), product.getPurchasePrice(),
                product.getMinStock(), product.isSerialTracked());
        // Controles avant toute modification de l'entite managee (voir CategoryService#update)
        String sku = TextNormalizer.upper(request.sku());
        String barcode = TextNormalizer.upper(request.barcode());
        if (productRepository.existsBySkuAndIdNot(sku, id)) {
            throw duplicate("SKU", sku);
        }
        if (barcode != null && productRepository.existsByBarcodeAndIdNot(barcode, id)) {
            throw duplicate("code-barres", barcode);
        }
        // Activer le suivi est toujours possible (l'IMEI est facultatif) ; le desactiver laisserait
        // des unites identifiees en stock sans suivi.
        if (!request.serialTracked() && product.isSerialTracked() && product.getQuantity() > 0) {
            throw new BusinessException(
                    "Impossible de désactiver le suivi à l'unité d'un produit en stock (%d en stock)"
                            .formatted(product.getQuantity()));
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

    /** SKU d'un produit cree sans SKU : PRD-000001, PRD-000002... (en sautant un SKU saisi a la main). */
    private String generateSku() {
        String sku;
        do {
            sku = "PRD-%06d".formatted(productRepository.nextSkuNumber());
        } while (productRepository.existsBySku(sku));
        return sku;
    }

    private Product getEntity(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", id));
    }

    private static BusinessException duplicate(String field, String value) {
        return new BusinessException("Un produit avec le %s '%s' existe déjà".formatted(field, value));
    }
}
