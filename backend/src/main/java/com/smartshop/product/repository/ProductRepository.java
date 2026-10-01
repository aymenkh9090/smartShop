package com.smartshop.product.repository;

import com.smartshop.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    /** Charge la categorie dans la meme requete (evite le N+1 sur les listes). */
    @Override
    @EntityGraph(attributePaths = "category")
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    boolean existsByBarcode(String barcode);

    boolean existsByBarcodeAndIdNot(String barcode, Long id);

    long countByCategoryId(Long categoryId);
}
