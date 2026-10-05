package com.smartshop.customercredit.controller;

import com.smartshop.common.credit.CreditCancelRequest;
import com.smartshop.common.dto.PageResponse;
import com.smartshop.common.payment.CreditPaymentRequest;
import com.smartshop.common.payment.RescheduleRequest;
import com.smartshop.customercredit.dto.CustomerBalanceResponse;
import com.smartshop.customercredit.dto.CustomerCreditFilter;
import com.smartshop.customercredit.dto.CustomerCreditRequest;
import com.smartshop.customercredit.dto.CustomerCreditResponse;
import com.smartshop.customercredit.dto.CustomerCreditSummaryResponse;
import com.smartshop.customercredit.service.CustomerCreditService;
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
 * Credits clients : consultation, saisie et encaissement par les SELLER ; report d'echeance et
 * annulation par les MANAGER. Les credits issus d'une vente sont adresses par l'id de la vente,
 * les credits saisis a la main sous /manual/{id}.
 */
@RestController
@RequestMapping("/api/customer-credits")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
public class CustomerCreditController {

    private final CustomerCreditService creditService;

    @GetMapping
    public PageResponse<CustomerCreditSummaryResponse> search(
            CustomerCreditFilter filter,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return creditService.search(filter, pageable);
    }

    @GetMapping("/balances")
    public PageResponse<CustomerBalanceResponse> balances(@RequestParam(required = false) Long customerId,
                                                          @PageableDefault(size = 20) Pageable pageable) {
        return creditService.balances(customerId, pageable);
    }

    @PostMapping
    public ResponseEntity<CustomerCreditResponse> create(@Valid @RequestBody CustomerCreditRequest request) {
        CustomerCreditResponse created = creditService.create(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/manual/{id}")
                .buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/manual/{id}")
    public CustomerCreditResponse findManual(@PathVariable Long id) {
        return creditService.findManual(id);
    }

    @PostMapping("/manual/{id}/payments")
    public CustomerCreditResponse payManual(@PathVariable Long id, @Valid @RequestBody CreditPaymentRequest request) {
        return creditService.payManual(id, request);
    }

    @PatchMapping("/manual/{id}/due-date")
    @PreAuthorize("hasRole('MANAGER')")
    public CustomerCreditResponse rescheduleManual(@PathVariable Long id,
                                                   @Valid @RequestBody RescheduleRequest request) {
        return creditService.rescheduleManual(id, request);
    }

    @PostMapping("/manual/{id}/cancel")
    @PreAuthorize("hasRole('MANAGER')")
    public CustomerCreditResponse cancelManual(@PathVariable Long id, @Valid @RequestBody CreditCancelRequest request) {
        return creditService.cancelManual(id, request);
    }

    @GetMapping("/{saleId}")
    public CustomerCreditResponse findById(@PathVariable Long saleId) {
        return creditService.findById(saleId);
    }

    @PostMapping("/{saleId}/payments")
    public CustomerCreditResponse receivePayment(@PathVariable Long saleId,
                                                 @Valid @RequestBody CreditPaymentRequest request) {
        return creditService.receivePayment(saleId, request);
    }

    @PatchMapping("/{saleId}/due-date")
    @PreAuthorize("hasRole('MANAGER')")
    public CustomerCreditResponse reschedule(@PathVariable Long saleId, @Valid @RequestBody RescheduleRequest request) {
        return creditService.reschedule(saleId, request);
    }
}
