package com.smartshop.sale.mapper;

import com.smartshop.common.mapping.MapStructConfig;
import com.smartshop.common.payment.PaymentResponse;
import com.smartshop.customer.entity.Customer;
import com.smartshop.sale.dto.SaleResponse;
import com.smartshop.sale.dto.SaleSummaryResponse;
import com.smartshop.sale.entity.Sale;
import com.smartshop.sale.entity.SaleItem;
import com.smartshop.sale.entity.SalePayment;
import com.smartshop.stock.entity.ProductUnit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapStructConfig.class)
public interface SaleMapper {

    @Mapping(target = "createdBy", source = "createdBy.username")
    @Mapping(target = "cancelledBy", source = "cancelledBy.username")
    SaleResponse toResponse(Sale sale);

    @Mapping(target = "createdBy", source = "createdBy.username")
    SaleSummaryResponse toSummary(Sale sale);

    SaleResponse.CustomerSummary toCustomerSummary(Customer customer);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productSku", source = "product.sku")
    @Mapping(target = "productName", source = "product.name")
    SaleResponse.Item toItem(SaleItem item);

    SaleResponse.Unit toUnit(ProductUnit unit);

    @Mapping(target = "createdBy", source = "createdBy.username")
    PaymentResponse toPayment(SalePayment payment);
}
