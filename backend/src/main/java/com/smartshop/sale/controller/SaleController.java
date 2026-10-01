package com.smartshop.sale.controller;

import com.smartshop.common.dto.PageResponse;
import com.smartshop.sale.dto.SaleCancelRequest;
import com.smartshop.sale.dto.SaleFilter;
import com.smartshop.sale.dto.SaleRequest;
import com.smartshop.sale.dto.SaleResponse;
import com.smartshop.sale.dto.SaleSummaryResponse;
import com.smartshop.sale.service.SaleService;
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

/**
 * Ventes : SELLER (et au-dessus) ; l'annulation est reservee aux MANAGER.
 */
@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
public class SaleController {

    private final SaleService saleService;

    @GetMapping
    public PageResponse<SaleSummaryResponse> search(
            SaleFilter filter,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return saleService.search(filter, pageable);
    }

    @GetMapping("/{id}")
    public SaleResponse findById(@PathVariable Long id) {
        return saleService.findById(id);
    }

    @PostMapping
    public ResponseEntity<SaleResponse> create(@Valid @RequestBody SaleRequest request) {
        SaleResponse created = saleService.create(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri()).body(created);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('MANAGER')")
    public SaleResponse cancel(@PathVariable Long id, @Valid @RequestBody SaleCancelRequest request) {
        return saleService.cancel(id, request);
    }
}
