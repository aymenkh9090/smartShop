package com.smartshop.sale.repository;

import com.smartshop.sale.entity.Sale;
import com.smartshop.sale.entity.SaleItem;
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

import java.util.List;
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

    /** Lignes de ventes non annulees ayant vendu cette unite, la plus recente en premier (garantie SAV). */
    @Query("""
            select i from SaleItem i join fetch i.sale s join i.units u
            where u.id = :unitId and s.status = com.smartshop.sale.entity.SaleStatus.COMPLETED
            order by s.createdAt desc""")
    List<SaleItem> findCompletedItemsByUnit(@Param("unitId") Long unitId);
}
