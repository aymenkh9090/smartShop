package com.smartshop.purchase.controller;

import com.smartshop.common.dto.PageResponse;
import com.smartshop.purchase.dto.PurchaseFilter;
import com.smartshop.purchase.dto.PurchaseRequest;
import com.smartshop.purchase.dto.PurchaseResponse;
import com.smartshop.purchase.dto.PurchaseSummaryResponse;
import com.smartshop.purchase.service.PurchaseService;
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

/** Achats fournisseurs : reserves aux MANAGER (et ADMIN). */
@RestController
@RequestMapping("/api/purchases")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
public class PurchaseController {

    private final PurchaseService purchaseService;

    @GetMapping
    public PageResponse<PurchaseSummaryResponse> search(
            PurchaseFilter filter,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return purchaseService.search(filter, pageable);
    }

    @GetMapping("/{id}")
    public PurchaseResponse findById(@PathVariable Long id) {
        return purchaseService.findById(id);
    }

    @PostMapping
    public ResponseEntity<PurchaseResponse> create(@Valid @RequestBody PurchaseRequest request) {
        PurchaseResponse created = purchaseService.create(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri()).body(created);
    }
}
