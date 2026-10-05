package com.smartshop.customercredit.mapper;

import com.smartshop.common.mapping.MapStructConfig;
import com.smartshop.common.payment.CreditStatus;
import com.smartshop.common.payment.PaymentResponse;
import com.smartshop.customercredit.dto.CustomerCreditResponse;
import com.smartshop.customercredit.dto.CustomerCreditSummaryResponse;
import com.smartshop.customercredit.entity.CustomerCredit;
import com.smartshop.customercredit.entity.CustomerCreditEntry;
import com.smartshop.customercredit.entity.CustomerCreditPayment;
import com.smartshop.sale.entity.Sale;
import com.smartshop.sale.entity.SalePayment;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDate;

@Mapper(config = MapStructConfig.class, imports = CreditStatus.class)
public interface CustomerCreditMapper {

    @Mapping(target = "id", source = "sourceId")
    @Mapping(target = "createdBy", source = "createdBy.username")
    @Mapping(target = "status", expression =
            "java(CreditStatus.of(entry.isCancelled(), entry.getPaymentStatus(), entry.getDueDate(), today))")
    CustomerCreditSummaryResponse toSummary(CustomerCreditEntry entry, @Context LocalDate today);

    @Mapping(target = "source", constant = "SALE")
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "createdBy", source = "createdBy.username")
    @Mapping(target = "status", expression =
            "java(CreditStatus.of(sale.isCancelled(), sale.getPaymentStatus(), sale.getDueDate(), today))")
    CustomerCreditResponse toResponse(Sale sale, @Context LocalDate today);

    @Mapping(target = "source", constant = "MANUAL")
    @Mapping(target = "total", source = "amount")
    @Mapping(target = "createdAt", source = "creditedAt")
    @Mapping(target = "createdBy", source = "createdBy.username")
    @Mapping(target = "status", expression =
            "java(CreditStatus.of(credit.isCancelled(), credit.getPaymentStatus(), credit.getDueDate(), today))")
    CustomerCreditResponse toResponse(CustomerCredit credit, @Context LocalDate today);

    CustomerCreditResponse.Customer toCustomer(com.smartshop.customer.entity.Customer customer);

    @Mapping(target = "createdBy", source = "createdBy.username")
    PaymentResponse toPayment(SalePayment payment);

    @Mapping(target = "createdBy", source = "createdBy.username")
    PaymentResponse toPayment(CustomerCreditPayment payment);
}
