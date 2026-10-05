package com.smartshop.suppliercredit.mapper;

import com.smartshop.common.mapping.MapStructConfig;
import com.smartshop.common.payment.CreditStatus;
import com.smartshop.common.payment.PaymentResponse;
import com.smartshop.purchase.entity.Purchase;
import com.smartshop.purchase.entity.PurchasePayment;
import com.smartshop.suppliercredit.dto.SupplierCreditResponse;
import com.smartshop.suppliercredit.dto.SupplierCreditSummaryResponse;
import com.smartshop.suppliercredit.entity.SupplierCredit;
import com.smartshop.suppliercredit.entity.SupplierCreditEntry;
import com.smartshop.suppliercredit.entity.SupplierCreditPayment;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDate;

@Mapper(config = MapStructConfig.class, imports = CreditStatus.class)
public interface SupplierCreditMapper {

    @Mapping(target = "id", source = "sourceId")
    @Mapping(target = "createdBy", source = "createdBy.username")
    @Mapping(target = "status", expression =
            "java(CreditStatus.of(entry.isCancelled(), entry.getPaymentStatus(), entry.getDueDate(), today))")
    SupplierCreditSummaryResponse toSummary(SupplierCreditEntry entry, @Context LocalDate today);

    @Mapping(target = "source", constant = "PURCHASE")
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "cancelReason", ignore = true)
    @Mapping(target = "createdBy", source = "createdBy.username")
    @Mapping(target = "status", expression =
            "java(CreditStatus.of(false, purchase.getPaymentStatus(), purchase.getDueDate(), today))")
    SupplierCreditResponse toResponse(Purchase purchase, @Context LocalDate today);

    @Mapping(target = "source", constant = "MANUAL")
    @Mapping(target = "supplierInvoiceRef", ignore = true)
    @Mapping(target = "total", source = "amount")
    @Mapping(target = "createdAt", source = "creditedAt")
    @Mapping(target = "createdBy", source = "createdBy.username")
    @Mapping(target = "status", expression =
            "java(CreditStatus.of(credit.isCancelled(), credit.getPaymentStatus(), credit.getDueDate(), today))")
    SupplierCreditResponse toResponse(SupplierCredit credit, @Context LocalDate today);

    SupplierCreditResponse.Supplier toSupplier(com.smartshop.supplier.entity.Supplier supplier);

    @Mapping(target = "createdBy", source = "createdBy.username")
    PaymentResponse toPayment(PurchasePayment payment);

    @Mapping(target = "createdBy", source = "createdBy.username")
    PaymentResponse toPayment(SupplierCreditPayment payment);
}
