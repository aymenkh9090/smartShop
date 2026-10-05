package com.smartshop.suppliercredit.controller;

import com.smartshop.common.credit.CreditCancelRequest;
import com.smartshop.common.dto.PageResponse;
import com.smartshop.common.payment.CreditPaymentRequest;
import com.smartshop.common.payment.RescheduleRequest;
import com.smartshop.suppliercredit.dto.SupplierBalanceResponse;
import com.smartshop.suppliercredit.dto.SupplierCreditFilter;
import com.smartshop.suppliercredit.dto.SupplierCreditRequest;
import com.smartshop.suppliercredit.dto.SupplierCreditResponse;
import com.smartshop.suppliercredit.dto.SupplierCreditSummaryResponse;
import com.smartshop.suppliercredit.service.SupplierCreditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Credits fournisseurs : reserves aux MANAGER (et ADMIN). Les credits issus d'un achat sont adresses
 * par l'id de l'achat, les credits saisis a la main sous /manual/{id}.
 */
@RestController
@RequestMapping("/api/supplier-credits")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
public class SupplierCreditController {

    private final SupplierCreditService creditService;

    @GetMapping
    public PageResponse<SupplierCreditSummaryResponse> search(
            SupplierCreditFilter filter,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return creditService.search(filter, pageable);
    }

    @GetMapping("/balances")
    public PageResponse<SupplierBalanceResponse> balances(@RequestParam(required = false) Long supplierId,
                                                          @PageableDefault(size = 20) Pageable pageable) {
        return creditService.balances(supplierId, pageable);
    }

    @PostMapping
    public ResponseEntity<SupplierCreditResponse> create(@Valid @RequestBody SupplierCreditRequest request) {
        SupplierCreditResponse created = creditService.create(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/manual/{id}")
                .buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/manual/{id}")
    public SupplierCreditResponse findManual(@PathVariable Long id) {
        return creditService.findManual(id);
    }

    @PostMapping("/manual/{id}/payments")
    public SupplierCreditResponse payManual(@PathVariable Long id, @Valid @RequestBody CreditPaymentRequest request) {
        return creditService.payManual(id, request);
    }

    @PatchMapping("/manual/{id}/due-date")
    public SupplierCreditResponse rescheduleManual(@PathVariable Long id,
                                                   @Valid @RequestBody RescheduleRequest request) {
        return creditService.rescheduleManual(id, request);
    }

    @PostMapping("/manual/{id}/cancel")
    public SupplierCreditResponse cancelManual(@PathVariable Long id, @Valid @RequestBody CreditCancelRequest request) {
        return creditService.cancelManual(id, request);
    }

    @GetMapping("/{purchaseId}")
    public SupplierCreditResponse findById(@PathVariable Long purchaseId) {
        return creditService.findById(purchaseId);
    }

    @PostMapping("/{purchaseId}/payments")
    public SupplierCreditResponse makePayment(@PathVariable Long purchaseId,
                                              @Valid @RequestBody CreditPaymentRequest request) {
        return creditService.makePayment(purchaseId, request);
    }

    @PatchMapping("/{purchaseId}/due-date")
    public SupplierCreditResponse reschedule(@PathVariable Long purchaseId,
                                             @Valid @RequestBody RescheduleRequest request) {
        return creditService.reschedule(purchaseId, request);
    }
}
