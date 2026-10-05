package com.smartshop.customer.service;

import com.smartshop.common.exception.BusinessException;
import com.smartshop.customer.dto.CustomerRequest;
import com.smartshop.customer.dto.CustomerResponse;
import com.smartshop.customer.entity.Customer;
import com.smartshop.customer.mapper.CustomerMapper;
import com.smartshop.customer.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    private CustomerService service;

    @BeforeEach
    void setUp() {
        service = new CustomerService(customerRepository, Mappers.getMapper(CustomerMapper.class));
    }

    @Test
    void create_normalizesPhoneEmailAndCin() {
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerResponse response = service.create(
                new CustomerRequest(" Ali ", "Ben Salah", "+216 22 333 444", " Ali@Mail.TN ", " ", "ab123456", null));

        assertThat(response.firstName()).isEqualTo("Ali");
        assertThat(response.phone()).isEqualTo("+21622333444");
        assertThat(response.email()).isEqualTo("ali@mail.tn");
        assertThat(response.address()).isNull();
        assertThat(response.cin()).isEqualTo("AB123456");
    }

    @Test
    void create_withExistingPhone_isRejected() {
        when(customerRepository.existsByPhone("22333444")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("22 333 444", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("téléphone");
        verify(customerRepository, never()).save(any());
    }

    @Test
    void create_withExistingCin_isRejected() {
        when(customerRepository.existsByCin("AB123456")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("22333444", "ab123456")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("CIN");
    }

    @Test
    void update_withPhoneOfAnotherCustomer_isRejected() {
        Customer existing = Mappers.getMapper(CustomerMapper.class).toEntity(request("22333444", null));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(customerRepository.existsByPhoneAndIdNot("98765432", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, request("98765432", null)))
                .isInstanceOf(BusinessException.class);
    }

    private static CustomerRequest request(String phone, String cin) {
        return new CustomerRequest("Ali", "Ben Salah", phone, null, null, cin, null);
    }
}
