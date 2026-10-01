package com.smartshop.suppliercredit.controller;

import com.smartshop.common.dto.PageResponse;
import com.smartshop.common.payment.PaymentRequest;
import com.smartshop.common.payment.RescheduleRequest;
import com.smartshop.suppliercredit.dto.SupplierBalanceResponse;
import com.smartshop.suppliercredit.dto.SupplierCreditFilter;
import com.smartshop.suppliercredit.dto.SupplierCreditResponse;
import com.smartshop.suppliercredit.service.SupplierCreditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Credits fournisseurs : reserves aux MANAGER (et ADMIN). */
@RestController
@RequestMapping("/api/supplier-credits")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
public class SupplierCreditController {

    private final SupplierCreditService creditService;

    @GetMapping
    public PageResponse<SupplierCreditResponse> search(
            SupplierCreditFilter filter,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return creditService.search(filter, pageable);
    }

    @GetMapping("/balances")
    public PageResponse<SupplierBalanceResponse> balances(@RequestParam(required = false) Long supplierId,
                                                          @PageableDefault(size = 20) Pageable pageable) {
        return creditService.balances(supplierId, pageable);
    }

    @GetMapping("/{purchaseId}")
    public SupplierCreditResponse findById(@PathVariable Long purchaseId) {
        return creditService.findById(purchaseId);
    }

    @PostMapping("/{purchaseId}/payments")
    public SupplierCreditResponse makePayment(@PathVariable Long purchaseId,
                                              @Valid @RequestBody PaymentRequest request) {
        return creditService.makePayment(purchaseId, request);
    }

    @PatchMapping("/{purchaseId}/due-date")
    public SupplierCreditResponse reschedule(@PathVariable Long purchaseId,
                                             @Valid @RequestBody RescheduleRequest request) {
        return creditService.reschedule(purchaseId, request);
    }
}
