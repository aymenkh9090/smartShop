package com.smartshop.customer.controller;

import com.smartshop.common.dto.PageResponse;
import com.smartshop.common.dto.StatusUpdateRequest;
import com.smartshop.customer.dto.CustomerFilter;
import com.smartshop.customer.dto.CustomerRequest;
import com.smartshop.customer.dto.CustomerResponse;
import com.smartshop.customer.service.CustomerService;
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

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    public PageResponse<CustomerResponse> search(CustomerFilter filter,
                                                 @PageableDefault(size = 20, sort = "lastName") Pageable pageable) {
        return customerService.search(filter, pageable);
    }

    @GetMapping("/{id}")
    public CustomerResponse findById(@PathVariable Long id) {
        return customerService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
        CustomerResponse created = customerService.create(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri()).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public CustomerResponse update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return customerService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('MANAGER')")
    public CustomerResponse updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        return customerService.updateStatus(id, request.active());
    }
}
