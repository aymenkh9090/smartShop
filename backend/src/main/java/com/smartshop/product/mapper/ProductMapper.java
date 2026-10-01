package com.smartshop.product.mapper;

import com.smartshop.category.entity.Category;
import com.smartshop.common.mapping.MapStructConfig;
import com.smartshop.product.dto.ProductRequest;
import com.smartshop.product.dto.ProductResponse;
import com.smartshop.product.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface ProductMapper {

    ProductResponse toResponse(Product product);

    ProductResponse.CategorySummary toSummary(Category category);

    @Mapping(target = "sku", qualifiedByName = "upper")
    @Mapping(target = "barcode", qualifiedByName = "upper")
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "active", ignore = true)
    Product toEntity(ProductRequest request);

    @Mapping(target = "sku", qualifiedByName = "upper")
    @Mapping(target = "barcode", qualifiedByName = "upper")
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "active", ignore = true)
    void update(ProductRequest request, @MappingTarget Product product);
}
