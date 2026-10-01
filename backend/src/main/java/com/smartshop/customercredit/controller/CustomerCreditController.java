package com.smartshop.customercredit.controller;

import com.smartshop.common.dto.PageResponse;
import com.smartshop.common.payment.PaymentRequest;
import com.smartshop.customercredit.dto.CustomerBalanceResponse;
import com.smartshop.customercredit.dto.CustomerCreditFilter;
import com.smartshop.customercredit.dto.CustomerCreditResponse;
import com.smartshop.common.payment.RescheduleRequest;
import com.smartshop.customercredit.service.CustomerCreditService;
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

/**
 * Credits clients : consultation et encaissement par les SELLER ; report d'echeance par les MANAGER.
 */
@RestController
@RequestMapping("/api/customer-credits")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
public class CustomerCreditController {

    private final CustomerCreditService creditService;

    @GetMapping
    public PageResponse<CustomerCreditResponse> search(
            CustomerCreditFilter filter,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return creditService.search(filter, pageable);
    }

    @GetMapping("/balances")
    public PageResponse<CustomerBalanceResponse> balances(@RequestParam(required = false) Long customerId,
                                                          @PageableDefault(size = 20) Pageable pageable) {
        return creditService.balances(customerId, pageable);
    }

    @GetMapping("/{saleId}")
    public CustomerCreditResponse findById(@PathVariable Long saleId) {
        return creditService.findById(saleId);
    }

    @PostMapping("/{saleId}/payments")
    public CustomerCreditResponse receivePayment(@PathVariable Long saleId,
                                                 @Valid @RequestBody PaymentRequest request) {
        return creditService.receivePayment(saleId, request);
    }

    @PatchMapping("/{saleId}/due-date")
    @PreAuthorize("hasRole('MANAGER')")
    public CustomerCreditResponse reschedule(@PathVariable Long saleId, @Valid @RequestBody RescheduleRequest request) {
        return creditService.reschedule(saleId, request);
    }
}
