package com.smartshop.aftersales.controller;

import com.smartshop.aftersales.dto.AfterSalesCancelRequest;
import com.smartshop.aftersales.dto.AfterSalesCreateRequest;
import com.smartshop.aftersales.dto.AfterSalesDeliverRequest;
import com.smartshop.aftersales.dto.AfterSalesFilter;
import com.smartshop.aftersales.dto.AfterSalesPartRequest;
import com.smartshop.aftersales.dto.AfterSalesResponse;
import com.smartshop.aftersales.dto.AfterSalesStatusRequest;
import com.smartshop.aftersales.dto.AfterSalesSummaryResponse;
import com.smartshop.aftersales.dto.AfterSalesWorkRequest;
import com.smartshop.aftersales.dto.TechnicianAssignRequest;
import com.smartshop.aftersales.dto.TechnicianResponse;
import com.smartshop.aftersales.service.AfterSalesService;
import com.smartshop.common.dto.PageResponse;
import com.smartshop.common.payment.PaymentRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

/**
 * SAV : lecture SELLER et TECHNICIAN ; reception, encaissement et livraison SELLER ;
 * diagnostic, pieces et statuts TECHNICIAN ; assignation et annulation MANAGER.
 */
@RestController
@RequestMapping("/api/after-sales")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SELLER', 'TECHNICIAN')")
public class AfterSalesController {

    private final AfterSalesService afterSalesService;

    @GetMapping
    public PageResponse<AfterSalesSummaryResponse> search(
            AfterSalesFilter filter,
            @PageableDefault(size = 20, sort = "receivedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return afterSalesService.search(filter, pageable);
    }

    @GetMapping("/{id}")
    public AfterSalesResponse findById(@PathVariable Long id) {
        return afterSalesService.findById(id);
    }

    /** Techniciens actifs, pour l'assignation. */
    @GetMapping("/technicians")
    public List<TechnicianResponse> technicians() {
        return afterSalesService.technicians();
    }

    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<AfterSalesResponse> create(@Valid @RequestBody AfterSalesCreateRequest request) {
        AfterSalesResponse created = afterSalesService.create(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri()).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public AfterSalesResponse updateWork(@PathVariable Long id, @Valid @RequestBody AfterSalesWorkRequest request) {
        return afterSalesService.updateWork(id, request);
    }

    @PatchMapping("/{id}/technician")
    @PreAuthorize("hasRole('MANAGER')")
    public AfterSalesResponse assignTechnician(@PathVariable Long id,
                                               @Valid @RequestBody TechnicianAssignRequest request) {
        return afterSalesService.assignTechnician(id, request);
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public AfterSalesResponse changeStatus(@PathVariable Long id, @Valid @RequestBody AfterSalesStatusRequest request) {
        return afterSalesService.changeStatus(id, request);
    }

    @PostMapping("/{id}/parts")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public AfterSalesResponse addPart(@PathVariable Long id, @Valid @RequestBody AfterSalesPartRequest request) {
        return afterSalesService.addPart(id, request);
    }

    @DeleteMapping("/{id}/parts/{partId}")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public AfterSalesResponse removePart(@PathVariable Long id, @PathVariable Long partId) {
        return afterSalesService.removePart(id, partId);
    }

    @PostMapping("/{id}/payments")
    @PreAuthorize("hasRole('SELLER')")
    public AfterSalesResponse receivePayment(@PathVariable Long id, @Valid @RequestBody PaymentRequest request) {
        return afterSalesService.receivePayment(id, request);
    }

    @PostMapping("/{id}/deliver")
    @PreAuthorize("hasRole('SELLER')")
    public AfterSalesResponse deliver(@PathVariable Long id, @Valid @RequestBody AfterSalesDeliverRequest request) {
        return afterSalesService.deliver(id, request);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('MANAGER')")
    public AfterSalesResponse cancel(@PathVariable Long id, @Valid @RequestBody AfterSalesCancelRequest request) {
        return afterSalesService.cancel(id, request);
    }
}
