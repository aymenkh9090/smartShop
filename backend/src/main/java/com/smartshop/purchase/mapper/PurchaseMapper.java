package com.smartshop.purchase.mapper;

import com.smartshop.common.mapping.MapStructConfig;
import com.smartshop.common.payment.PaymentResponse;
import com.smartshop.purchase.dto.PurchaseResponse;
import com.smartshop.purchase.dto.PurchaseSummaryResponse;
import com.smartshop.purchase.entity.Purchase;
import com.smartshop.purchase.entity.PurchaseItem;
import com.smartshop.purchase.entity.PurchasePayment;
import com.smartshop.stock.entity.ProductUnit;
import com.smartshop.supplier.entity.Supplier;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapStructConfig.class)
public interface PurchaseMapper {

    @Mapping(target = "createdBy", source = "createdBy.username")
    PurchaseResponse toResponse(Purchase purchase);

    @Mapping(target = "createdBy", source = "createdBy.username")
    PurchaseSummaryResponse toSummary(Purchase purchase);

    PurchaseResponse.SupplierSummary toSupplierSummary(Supplier supplier);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productSku", source = "product.sku")
    @Mapping(target = "productName", source = "product.name")
    PurchaseResponse.Item toItem(PurchaseItem item);

    PurchaseResponse.Unit toUnit(ProductUnit unit);

    @Mapping(target = "createdBy", source = "createdBy.username")
    PaymentResponse toPayment(PurchasePayment payment);
}
