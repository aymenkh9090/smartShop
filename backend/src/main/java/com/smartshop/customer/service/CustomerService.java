package com.smartshop.customer.service;

import com.smartshop.common.dto.PageResponse;
import com.smartshop.common.exception.BusinessException;
import com.smartshop.common.exception.ResourceNotFoundException;
import com.smartshop.common.mapping.TextNormalizer;
import com.smartshop.customer.dto.CustomerFilter;
import com.smartshop.customer.dto.CustomerRequest;
import com.smartshop.customer.dto.CustomerResponse;
import com.smartshop.customer.entity.Customer;
import com.smartshop.customer.mapper.CustomerMapper;
import com.smartshop.customer.repository.CustomerRepository;
import com.smartshop.customer.repository.CustomerSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> search(CustomerFilter filter, Pageable pageable) {
        return PageResponse.from(customerRepository.findAll(CustomerSpecifications.matching(filter), pageable)
                .map(customerMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public CustomerResponse findById(Long id) {
        return customerMapper.toResponse(getEntity(id));
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        Customer customer = customerMapper.toEntity(request);
        if (customerRepository.existsByPhone(customer.getPhone())) {
            throw duplicate("téléphone", customer.getPhone());
        }
        if (customer.getCin() != null && customerRepository.existsByCin(customer.getCin())) {
            throw duplicate("CIN", customer.getCin());
        }
        return customerMapper.toResponse(customerRepository.save(customer));
    }

    @Transactional
    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = getEntity(id);
        // Controles avant toute modification de l'entite managee (voir CategoryService#update)
        String phone = TextNormalizer.phone(request.phone());
        String cin = TextNormalizer.upper(request.cin());
        if (customerRepository.existsByPhoneAndIdNot(phone, id)) {
            throw duplicate("téléphone", phone);
        }
        if (cin != null && customerRepository.existsByCinAndIdNot(cin, id)) {
            throw duplicate("CIN", cin);
        }
        customerMapper.update(request, customer);
        return customerMapper.toResponse(customer);
    }

    @Transactional
    public CustomerResponse updateStatus(Long id, boolean active) {
        Customer customer = getEntity(id);
        customer.setActive(active);
        return customerMapper.toResponse(customer);
    }

    /** Client actif, pour une operation (vente, achat) qui le reference. */
    @Transactional(readOnly = true)
    public Customer getActiveEntity(Long id) {
        Customer entity = getEntity(id);
        if (!entity.isActive()) {
            throw new BusinessException("Client désactivé : opération impossible");
        }
        return entity;
    }

    private Customer getEntity(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client", id));
    }

    private static BusinessException duplicate(String field, String value) {
        return new BusinessException("Un client avec le %s '%s' existe déjà".formatted(field, value));
    }
}
