package com.smartshop.purchase.repository;

import com.smartshop.purchase.entity.Purchase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface PurchaseRepository extends JpaRepository<Purchase, Long>, JpaSpecificationExecutor<Purchase> {

    @Override
    @EntityGraph(attributePaths = {"supplier", "createdBy"})
    Page<Purchase> findAll(Specification<Purchase> spec, Pageable pageable);

    @Query(value = "select nextval('purchase_reference_seq')", nativeQuery = true)
    long nextReferenceNumber();
}
