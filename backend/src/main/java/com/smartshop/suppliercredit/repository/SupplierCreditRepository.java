package com.smartshop.suppliercredit.repository;

import com.smartshop.suppliercredit.entity.SupplierCredit;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/** Credits fournisseurs saisis a la main. */
public interface SupplierCreditRepository extends JpaRepository<SupplierCredit, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from SupplierCredit c where c.id = :id")
    Optional<SupplierCredit> findByIdForUpdate(@Param("id") Long id);

    @Query(value = "select nextval('supplier_credit_reference_seq')", nativeQuery = true)
    long nextReferenceNumber();
}
