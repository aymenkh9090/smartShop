package com.smartshop.customercredit.repository;

import com.smartshop.customercredit.dto.CustomerBalanceResponse;
import com.smartshop.sale.entity.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

/**
 * Vue "credits" sur les ventes : lecture et agregation des soldes clients.
 */
public interface CustomerCreditRepository extends JpaRepository<Sale, Long>, JpaSpecificationExecutor<Sale> {

    @Override
    @EntityGraph(attributePaths = {"customer", "createdBy"})
    Page<Sale> findAll(Specification<Sale> spec, Pageable pageable);

    /** Encours par client (credits non soldes, non annules), du plus gros au plus petit. */
    @Query(value = """
            select new com.smartshop.customercredit.dto.CustomerBalanceResponse(
                c.id, c.firstName, c.lastName, c.phone, count(s),
                sum(s.total - s.amountPaid),
                sum(case when s.dueDate < :today then s.total - s.amountPaid else null end),
                min(s.dueDate))
            from Sale s join s.customer c
            where s.onCredit = true
              and s.status = com.smartshop.sale.entity.SaleStatus.COMPLETED
              and s.paymentStatus <> com.smartshop.common.payment.PaymentStatus.PAID
              and (:customerId is null or c.id = :customerId)
            group by c.id, c.firstName, c.lastName, c.phone
            order by sum(s.total - s.amountPaid) desc, c.id
            """,
            countQuery = """
                    select count(distinct s.customer.id) from Sale s
                    where s.onCredit = true
                      and s.status = com.smartshop.sale.entity.SaleStatus.COMPLETED
                      and s.paymentStatus <> com.smartshop.common.payment.PaymentStatus.PAID
                      and (:customerId is null or s.customer.id = :customerId)
                    """)
    Page<CustomerBalanceResponse> findBalances(@Param("customerId") Long customerId, @Param("today") LocalDate today,
                                               Pageable pageable);
}
