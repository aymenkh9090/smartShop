package com.smartshop.stock.mapper;

import com.smartshop.common.mapping.MapStructConfig;
import com.smartshop.product.entity.Product;
import com.smartshop.stock.dto.ProductUnitResponse;
import com.smartshop.stock.dto.StockMovementResponse;
import com.smartshop.stock.entity.ProductUnit;
import com.smartshop.stock.entity.StockMovement;
import com.smartshop.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapStructConfig.class)
public interface StockMapper {

    StockMovementResponse toResponse(StockMovement movement);

    StockMovementResponse.ProductSummary toProductSummary(Product product);

    StockMovementResponse.UserSummary toUserSummary(User user);

    StockMovementResponse.UnitSummary toUnitSummary(ProductUnit unit);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productSku", source = "product.sku")
    @Mapping(target = "productName", source = "product.name")
    ProductUnitResponse toUnitResponse(ProductUnit unit);
}
