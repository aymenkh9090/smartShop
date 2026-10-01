package com.smartshop.customer.mapper;

import com.smartshop.common.mapping.MapStructConfig;
import com.smartshop.customer.dto.CustomerRequest;
import com.smartshop.customer.dto.CustomerResponse;
import com.smartshop.customer.entity.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface CustomerMapper {

    CustomerResponse toResponse(Customer customer);

    @Mapping(target = "phone", qualifiedByName = "phone")
    @Mapping(target = "email", qualifiedByName = "lower")
    @Mapping(target = "cin", qualifiedByName = "upper")
    @Mapping(target = "active", ignore = true)
    Customer toEntity(CustomerRequest request);

    @Mapping(target = "phone", qualifiedByName = "phone")
    @Mapping(target = "email", qualifiedByName = "lower")
    @Mapping(target = "cin", qualifiedByName = "upper")
    @Mapping(target = "active", ignore = true)
    void update(CustomerRequest request, @MappingTarget Customer customer);
}
