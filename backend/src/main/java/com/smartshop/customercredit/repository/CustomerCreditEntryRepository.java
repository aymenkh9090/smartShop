package com.smartshop.customercredit.repository;

import com.smartshop.customercredit.dto.CustomerBalanceResponse;
import com.smartshop.customercredit.entity.CustomerCreditEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

/**
 * Lecture de tous les credits clients (ventes a credit et credits saisis) et agregation des soldes.
 */
public interface CustomerCreditEntryRepository extends Repository<CustomerCreditEntry, String>,
        JpaSpecificationExecutor<CustomerCreditEntry> {

    @Override
    @EntityGraph(attributePaths = {"customer", "createdBy"})
    Page<CustomerCreditEntry> findAll(Specification<CustomerCreditEntry> spec, Pageable pageable);

    /** Encours par client (credits non soldes, non annules), du plus gros au plus petit. */
    @Query(value = """
            select new com.smartshop.customercredit.dto.CustomerBalanceResponse(
                c.id, c.firstName, c.lastName, c.phone, count(e),
                sum(e.total - e.amountPaid),
                sum(case when e.dueDate < :today then e.total - e.amountPaid else null end),
                min(e.dueDate))
            from CustomerCreditEntry e join e.customer c
            where e.cancelled = false
              and e.paymentStatus <> com.smartshop.common.payment.PaymentStatus.PAID
              and (:customerId is null or c.id = :customerId)
            group by c.id, c.firstName, c.lastName, c.phone
            order by sum(e.total - e.amountPaid) desc, c.id
            """,
            countQuery = """
                    select count(distinct e.customer.id) from CustomerCreditEntry e
                    where e.cancelled = false
                      and e.paymentStatus <> com.smartshop.common.payment.PaymentStatus.PAID
                      and (:customerId is null or e.customer.id = :customerId)
                    """)
    Page<CustomerBalanceResponse> findBalances(@Param("customerId") Long customerId, @Param("today") LocalDate today,
                                               Pageable pageable);
}
