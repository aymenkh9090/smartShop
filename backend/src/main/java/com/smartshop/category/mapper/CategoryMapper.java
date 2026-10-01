package com.smartshop.category.mapper;

import com.smartshop.category.dto.CategoryRequest;
import com.smartshop.category.dto.CategoryResponse;
import com.smartshop.category.entity.Category;
import com.smartshop.common.mapping.MapStructConfig;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface CategoryMapper {

    CategoryResponse toResponse(Category category);

    Category toEntity(CategoryRequest request);

    void update(CategoryRequest request, @MappingTarget Category category);
}
