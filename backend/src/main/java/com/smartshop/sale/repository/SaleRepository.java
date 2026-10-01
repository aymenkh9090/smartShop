package com.smartshop.sale.repository;

import com.smartshop.sale.entity.Sale;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long>, JpaSpecificationExecutor<Sale> {

    @Override
    @EntityGraph(attributePaths = {"customer", "createdBy"})
    Page<Sale> findAll(Specification<Sale> spec, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Sale s where s.id = :id")
    Optional<Sale> findByIdForUpdate(@Param("id") Long id);

    @Query(value = "select nextval('sale_reference_seq')", nativeQuery = true)
    long nextReferenceNumber();
}
