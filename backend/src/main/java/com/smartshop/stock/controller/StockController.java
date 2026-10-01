package com.smartshop.stock.controller;

import com.smartshop.common.dto.PageResponse;
import com.smartshop.stock.dto.InventoryRequest;
import com.smartshop.stock.dto.InventoryResponse;
import com.smartshop.stock.dto.ProductUnitFilter;
import com.smartshop.stock.dto.ProductUnitResponse;
import com.smartshop.stock.dto.StockMovementFilter;
import com.smartshop.stock.dto.StockMovementRequest;
import com.smartshop.stock.dto.StockMovementResponse;
import com.smartshop.stock.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @GetMapping("/movements")
    public PageResponse<StockMovementResponse> searchMovements(
            StockMovementFilter filter,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return stockService.searchMovements(filter, pageable);
    }

    @GetMapping("/movements/{id}")
    public StockMovementResponse findMovement(@PathVariable Long id) {
        return stockService.findMovement(id);
    }

    @PostMapping("/movements")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<StockMovementResponse> recordMovement(@Valid @RequestBody StockMovementRequest request) {
        StockMovementResponse created = stockService.recordMovement(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri()).body(created);
    }

    @PostMapping("/inventory")
    @PreAuthorize("hasRole('MANAGER')")
    public InventoryResponse applyInventory(@Valid @RequestBody InventoryRequest request) {
        return stockService.applyInventory(request);
    }

    /** Recherche d'unites, notamment par IMEI ou numero de serie. */
    @GetMapping("/units")
    public PageResponse<ProductUnitResponse> searchUnits(
            ProductUnitFilter filter,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return stockService.searchUnits(filter, pageable);
    }
}
