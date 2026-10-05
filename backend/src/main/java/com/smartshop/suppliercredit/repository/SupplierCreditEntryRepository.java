package com.smartshop.suppliercredit.repository;

import com.smartshop.suppliercredit.dto.SupplierBalanceResponse;
import com.smartshop.suppliercredit.entity.SupplierCreditEntry;
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
 * Lecture de tous les credits fournisseurs (achats a credit et credits saisis) et agregation des soldes.
 */
public interface SupplierCreditEntryRepository extends Repository<SupplierCreditEntry, String>,
        JpaSpecificationExecutor<SupplierCreditEntry> {

    @Override
    @EntityGraph(attributePaths = {"supplier", "createdBy"})
    Page<SupplierCreditEntry> findAll(Specification<SupplierCreditEntry> spec, Pageable pageable);

    /** Encours par fournisseur (credits non soldes, non annules), du plus gros au plus petit. */
    @Query(value = """
            select new com.smartshop.suppliercredit.dto.SupplierBalanceResponse(
                f.id, f.name, f.phone, count(e),
                sum(e.total - e.amountPaid),
                sum(case when e.dueDate < :today then e.total - e.amountPaid else null end),
                min(e.dueDate))
            from SupplierCreditEntry e join e.supplier f
            where e.cancelled = false
              and e.paymentStatus <> com.smartshop.common.payment.PaymentStatus.PAID
              and (:supplierId is null or f.id = :supplierId)
            group by f.id, f.name, f.phone
            order by sum(e.total - e.amountPaid) desc, f.id
            """,
            countQuery = """
                    select count(distinct e.supplier.id) from SupplierCreditEntry e
                    where e.cancelled = false
                      and e.paymentStatus <> com.smartshop.common.payment.PaymentStatus.PAID
                      and (:supplierId is null or e.supplier.id = :supplierId)
                    """)
    Page<SupplierBalanceResponse> findBalances(@Param("supplierId") Long supplierId, @Param("today") LocalDate today,
                                               Pageable pageable);
}
