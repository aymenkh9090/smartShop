package com.smartshop.customercredit.mapper;

import com.smartshop.common.mapping.MapStructConfig;
import com.smartshop.common.payment.CreditStatus;
import com.smartshop.common.payment.PaymentResponse;
import com.smartshop.customercredit.dto.CustomerCreditResponse;
import com.smartshop.sale.entity.Sale;
import com.smartshop.sale.entity.SalePayment;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDate;

@Mapper(config = MapStructConfig.class, imports = CreditStatus.class)
public interface CustomerCreditMapper {

    @Mapping(target = "saleId", source = "id")
    @Mapping(target = "createdBy", source = "createdBy.username")
    @Mapping(target = "status", expression =
            "java(CreditStatus.of(sale.isCancelled(), sale.getPaymentStatus(), sale.getDueDate(), today))")
    CustomerCreditResponse toResponse(Sale sale, @Context LocalDate today);

    CustomerCreditResponse.Customer toCustomer(com.smartshop.customer.entity.Customer customer);

    @Mapping(target = "createdBy", source = "createdBy.username")
    PaymentResponse toPayment(SalePayment payment);
}
