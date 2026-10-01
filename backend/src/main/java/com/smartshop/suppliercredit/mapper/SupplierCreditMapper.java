package com.smartshop.suppliercredit.mapper;

import com.smartshop.common.mapping.MapStructConfig;
import com.smartshop.common.payment.CreditStatus;
import com.smartshop.common.payment.PaymentResponse;
import com.smartshop.purchase.entity.Purchase;
import com.smartshop.purchase.entity.PurchasePayment;
import com.smartshop.suppliercredit.dto.SupplierCreditResponse;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDate;

@Mapper(config = MapStructConfig.class, imports = CreditStatus.class)
public interface SupplierCreditMapper {

    @Mapping(target = "purchaseId", source = "id")
    @Mapping(target = "createdBy", source = "createdBy.username")
    @Mapping(target = "status", expression =
            "java(CreditStatus.of(false, purchase.getPaymentStatus(), purchase.getDueDate(), today))")
    SupplierCreditResponse toResponse(Purchase purchase, @Context LocalDate today);

    SupplierCreditResponse.Supplier toSupplier(com.smartshop.supplier.entity.Supplier supplier);

    @Mapping(target = "createdBy", source = "createdBy.username")
    PaymentResponse toPayment(PurchasePayment payment);
}
