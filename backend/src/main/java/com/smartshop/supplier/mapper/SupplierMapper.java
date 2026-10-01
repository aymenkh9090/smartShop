package com.smartshop.supplier.mapper;

import com.smartshop.common.mapping.MapStructConfig;
import com.smartshop.supplier.dto.SupplierRequest;
import com.smartshop.supplier.dto.SupplierResponse;
import com.smartshop.supplier.entity.Supplier;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface SupplierMapper {

    SupplierResponse toResponse(Supplier supplier);

    @Mapping(target = "phone", qualifiedByName = "phone")
    @Mapping(target = "email", qualifiedByName = "lower")
    @Mapping(target = "taxId", qualifiedByName = "upper")
    @Mapping(target = "active", ignore = true)
    Supplier toEntity(SupplierRequest request);

    @Mapping(target = "phone", qualifiedByName = "phone")
    @Mapping(target = "email", qualifiedByName = "lower")
    @Mapping(target = "taxId", qualifiedByName = "upper")
    @Mapping(target = "active", ignore = true)
    void update(SupplierRequest request, @MappingTarget Supplier supplier);
}
