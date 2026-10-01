package com.smartshop.suppliercredit.repository;

import com.smartshop.purchase.entity.Purchase;
import com.smartshop.suppliercredit.dto.SupplierBalanceResponse;
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

import java.time.LocalDate;
import java.util.Optional;

/**
 * Vue "credits" sur les achats : lecture, verrouillage pour paiement et agregation des soldes fournisseurs.
 */
public interface SupplierCreditRepository extends JpaRepository<Purchase, Long>, JpaSpecificationExecutor<Purchase> {

    @Override
    @EntityGraph(attributePaths = {"supplier", "createdBy"})
    Page<Purchase> findAll(Specification<Purchase> spec, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Purchase p where p.id = :id")
    Optional<Purchase> findByIdForUpdate(@Param("id") Long id);

    /** Encours par fournisseur (credits non soldes), du plus gros au plus petit. */
    @Query(value = """
            select new com.smartshop.suppliercredit.dto.SupplierBalanceResponse(
                f.id, f.name, f.phone, count(p),
                sum(p.total - p.amountPaid),
                sum(case when p.dueDate < :today then p.total - p.amountPaid else null end),
                min(p.dueDate))
            from Purchase p join p.supplier f
            where p.onCredit = true
              and p.paymentStatus <> com.smartshop.common.payment.PaymentStatus.PAID
              and (:supplierId is null or f.id = :supplierId)
            group by f.id, f.name, f.phone
            order by sum(p.total - p.amountPaid) desc, f.id
            """,
            countQuery = """
                    select count(distinct p.supplier.id) from Purchase p
                    where p.onCredit = true
                      and p.paymentStatus <> com.smartshop.common.payment.PaymentStatus.PAID
                      and (:supplierId is null or p.supplier.id = :supplierId)
                    """)
    Page<SupplierBalanceResponse> findBalances(@Param("supplierId") Long supplierId, @Param("today") LocalDate today,
                                               Pageable pageable);
}
