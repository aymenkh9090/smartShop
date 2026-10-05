package com.smartshop.customercredit.repository;

import com.smartshop.customercredit.entity.CustomerCredit;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/** Credits clients saisis a la main. */
public interface CustomerCreditRepository extends JpaRepository<CustomerCredit, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CustomerCredit c where c.id = :id")
    Optional<CustomerCredit> findByIdForUpdate(@Param("id") Long id);

    @Query(value = "select nextval('customer_credit_reference_seq')", nativeQuery = true)
    long nextReferenceNumber();
}
