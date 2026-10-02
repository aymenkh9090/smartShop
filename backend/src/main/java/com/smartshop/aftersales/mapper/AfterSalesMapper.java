package com.smartshop.aftersales.mapper;

import com.smartshop.aftersales.dto.AfterSalesResponse;
import com.smartshop.aftersales.dto.AfterSalesSummaryResponse;
import com.smartshop.aftersales.dto.TechnicianResponse;
import com.smartshop.aftersales.entity.AfterSalesPart;
import com.smartshop.aftersales.entity.AfterSalesPayment;
import com.smartshop.aftersales.entity.AfterSalesStatusChange;
import com.smartshop.aftersales.entity.AfterSalesTicket;
import com.smartshop.common.mapping.MapStructConfig;
import com.smartshop.common.payment.PaymentResponse;
import com.smartshop.customer.entity.Customer;
import com.smartshop.product.entity.Product;
import com.smartshop.stock.entity.ProductUnit;
import com.smartshop.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapStructConfig.class)
public interface AfterSalesMapper {

    @Mapping(target = "unitId", source = "unit.id")
    @Mapping(target = "saleId", source = "sale.id")
    @Mapping(target = "saleReference", source = "sale.reference")
    @Mapping(target = "createdBy", source = "createdBy.username")
    AfterSalesResponse toResponse(AfterSalesTicket ticket);

    AfterSalesSummaryResponse toSummary(AfterSalesTicket ticket);

    AfterSalesResponse.CustomerSummary toCustomer(Customer customer);

    AfterSalesResponse.ProductSummary toProduct(Product product);

    TechnicianResponse toTechnician(User user);

    @Mapping(target = "changedBy", source = "changedBy.username")
    AfterSalesResponse.StatusChange toStatusChange(AfterSalesStatusChange change);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productSku", source = "product.sku")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "addedBy", source = "addedBy.username")
    AfterSalesResponse.Part toPart(AfterSalesPart part);

    AfterSalesResponse.Unit toUnit(ProductUnit unit);

    @Mapping(target = "createdBy", source = "createdBy.username")
    PaymentResponse toPayment(AfterSalesPayment payment);
}
