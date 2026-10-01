package com.smartshop.stock.repository;

import com.smartshop.stock.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long>,
        JpaSpecificationExecutor<StockMovement> {

    @Override
    @EntityGraph(attributePaths = {"product", "createdBy"})
    Page<StockMovement> findAll(Specification<StockMovement> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"product", "createdBy"})
    Optional<StockMovement> findWithDetailsById(Long id);
}
