package com.smartshop.supplier.controller;

import com.smartshop.common.dto.PageResponse;
import com.smartshop.common.dto.StatusUpdateRequest;
import com.smartshop.supplier.dto.SupplierFilter;
import com.smartshop.supplier.dto.SupplierRequest;
import com.smartshop.supplier.dto.SupplierResponse;
import com.smartshop.supplier.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Fournisseurs : reserve aux MANAGER (et ADMIN), y compris la lecture.
 */
@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping
    public PageResponse<SupplierResponse> search(SupplierFilter filter,
                                                 @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return supplierService.search(filter, pageable);
    }

    @GetMapping("/{id}")
    public SupplierResponse findById(@PathVariable Long id) {
        return supplierService.findById(id);
    }

    @PostMapping
    public ResponseEntity<SupplierResponse> create(@Valid @RequestBody SupplierRequest request) {
        SupplierResponse created = supplierService.create(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri()).body(created);
    }

    @PutMapping("/{id}")
    public SupplierResponse update(@PathVariable Long id, @Valid @RequestBody SupplierRequest request) {
        return supplierService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public SupplierResponse updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        return supplierService.updateStatus(id, request.active());
    }
}
