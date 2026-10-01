package com.smartshop.supplier.service;

import com.smartshop.common.exception.BusinessException;
import com.smartshop.common.exception.ResourceNotFoundException;
import com.smartshop.supplier.dto.SupplierRequest;
import com.smartshop.supplier.entity.Supplier;
import com.smartshop.supplier.mapper.SupplierMapper;
import com.smartshop.supplier.repository.SupplierRepository;
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
class SupplierServiceTest {

    @Mock
    private SupplierRepository supplierRepository;

    private SupplierService service;

    @BeforeEach
    void setUp() {
        service = new SupplierService(supplierRepository, Mappers.getMapper(SupplierMapper.class));
    }

    @Test
    void create_normalizesTaxIdAndPhone() {
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(new SupplierRequest(" TechDistrib ", "71 000 000", null, null, "1234567/a", null));

        assertThat(response.name()).isEqualTo("TechDistrib");
        assertThat(response.phone()).isEqualTo("71000000");
        assertThat(response.taxId()).isEqualTo("1234567/A");
    }

    @Test
    void create_withExistingName_isRejected() {
        when(supplierRepository.existsByNameIgnoreCase("TechDistrib")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new SupplierRequest("TechDistrib", null, null, null, null, null)))
                .isInstanceOf(BusinessException.class);
        verify(supplierRepository, never()).save(any());
    }

    @Test
    void create_withExistingTaxId_isRejected() {
        when(supplierRepository.existsByTaxId("TAX-1")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new SupplierRequest("Autre", null, null, null, "tax-1", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("matricule fiscal");
    }

    @Test
    void updateStatus_unknownSupplier_throwsNotFound() {
        when(supplierRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateStatus(3L, false)).isInstanceOf(ResourceNotFoundException.class);
    }
}
