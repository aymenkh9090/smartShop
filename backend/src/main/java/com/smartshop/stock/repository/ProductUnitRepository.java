package com.smartshop.stock.repository;

import com.smartshop.stock.entity.ProductUnit;
import com.smartshop.stock.entity.ProductUnitStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductUnitRepository extends JpaRepository<ProductUnit, Long>, JpaSpecificationExecutor<ProductUnit> {

    @Override
    @EntityGraph(attributePaths = "product")
    Page<ProductUnit> findAll(Specification<ProductUnit> spec, Pageable pageable);

    @Query("select u.imei from ProductUnit u where u.imei in :imeis")
    List<String> findExistingImeis(@Param("imeis") Collection<String> imeis);

    @Query("select u.serialNumber from ProductUnit u where u.product.id = :productId and u.serialNumber in :serials")
    List<String> findExistingSerialNumbers(@Param("productId") Long productId,
                                           @Param("serials") Collection<String> serials);

    long countByProductIdAndStatus(Long productId, ProductUnitStatus status);

    Optional<ProductUnit> findByImei(String imei);

    /** Le numero de serie n'est unique que par produit : plusieurs unites peuvent correspondre. */
    List<ProductUnit> findBySerialNumber(String serialNumber);
}
