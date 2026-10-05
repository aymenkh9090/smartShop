package com.smartshop.supplier.service;

import com.smartshop.common.dto.PageResponse;
import com.smartshop.common.exception.BusinessException;
import com.smartshop.common.exception.ResourceNotFoundException;
import com.smartshop.common.mapping.TextNormalizer;
import com.smartshop.supplier.dto.SupplierFilter;
import com.smartshop.supplier.dto.SupplierRequest;
import com.smartshop.supplier.dto.SupplierResponse;
import com.smartshop.supplier.entity.Supplier;
import com.smartshop.supplier.mapper.SupplierMapper;
import com.smartshop.supplier.repository.SupplierRepository;
import com.smartshop.supplier.repository.SupplierSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierMapper supplierMapper;

    @Transactional(readOnly = true)
    public PageResponse<SupplierResponse> search(SupplierFilter filter, Pageable pageable) {
        return PageResponse.from(supplierRepository.findAll(SupplierSpecifications.matching(filter), pageable)
                .map(supplierMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public SupplierResponse findById(Long id) {
        return supplierMapper.toResponse(getEntity(id));
    }

    @Transactional
    public SupplierResponse create(SupplierRequest request) {
        Supplier supplier = supplierMapper.toEntity(request);
        if (supplierRepository.existsByNameIgnoreCase(supplier.getName())) {
            throw duplicate("nom", supplier.getName());
        }
        if (supplier.getTaxId() != null && supplierRepository.existsByTaxId(supplier.getTaxId())) {
            throw duplicate("matricule fiscal", supplier.getTaxId());
        }
        return supplierMapper.toResponse(supplierRepository.save(supplier));
    }

    @Transactional
    public SupplierResponse update(Long id, SupplierRequest request) {
        Supplier supplier = getEntity(id);
        // Controles avant toute modification de l'entite managee (voir CategoryService#update)
        String name = TextNormalizer.trimToNull(request.name());
        String taxId = TextNormalizer.upper(request.taxId());
        if (supplierRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw duplicate("nom", name);
        }
        if (taxId != null && supplierRepository.existsByTaxIdAndIdNot(taxId, id)) {
            throw duplicate("matricule fiscal", taxId);
        }
        supplierMapper.update(request, supplier);
        return supplierMapper.toResponse(supplier);
    }

    @Transactional
    public SupplierResponse updateStatus(Long id, boolean active) {
        Supplier supplier = getEntity(id);
        supplier.setActive(active);
        return supplierMapper.toResponse(supplier);
    }

    /** Fournisseur actif, pour une operation (vente, achat) qui le reference. */
    @Transactional(readOnly = true)
    public Supplier getActiveEntity(Long id) {
        Supplier entity = getEntity(id);
        if (!entity.isActive()) {
            throw new BusinessException("Fournisseur désactivé : opération impossible");
        }
        return entity;
    }

    private Supplier getEntity(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", id));
    }

    private static BusinessException duplicate(String field, String value) {
        return new BusinessException("Un fournisseur avec le %s '%s' existe déjà".formatted(field, value));
    }
}
