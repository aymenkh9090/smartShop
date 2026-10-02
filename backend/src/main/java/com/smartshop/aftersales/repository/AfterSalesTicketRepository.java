package com.smartshop.aftersales.repository;

import com.smartshop.aftersales.entity.AfterSalesTicket;
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

public interface AfterSalesTicketRepository extends JpaRepository<AfterSalesTicket, Long>,
        JpaSpecificationExecutor<AfterSalesTicket> {

    @Override
    @EntityGraph(attributePaths = {"customer", "technician"})
    Page<AfterSalesTicket> findAll(Specification<AfterSalesTicket> spec, Pageable pageable);

    /** Verrou pessimiste : serialise les operations concurrentes sur un meme ticket (pieces, paiements, statuts). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from AfterSalesTicket t where t.id = :id")
    Optional<AfterSalesTicket> findByIdForUpdate(@Param("id") Long id);

    @Query(value = "select nextval('after_sales_ticket_number_seq')", nativeQuery = true)
    long nextTicketNumber();
}
