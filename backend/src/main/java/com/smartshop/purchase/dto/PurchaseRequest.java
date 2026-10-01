package com.smartshop.purchase.dto;

import com.smartshop.common.payment.PaymentRequest;
import com.smartshop.stock.dto.ProductUnitRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Reception d'un achat fournisseur : le stock est alimente immediatement.
 */
public record PurchaseRequest(
        @NotNull Long supplierId,
        @Size(max = 100) String supplierInvoiceRef,
        @NotEmpty @Size(max = 200) @Valid List<@NotNull Item> items,
        @Size(max = 10) @Valid List<@NotNull PaymentRequest> payments,
        LocalDate dueDate,
        @Size(max = 1000) String notes
) {

    public List<PaymentRequest> paymentsOrEmpty() {
        return payments == null ? List.of() : payments;
    }

    /**
     * @param units unites recues (IMEI / numero de serie) pour un produit suivi : une par article
     */
    public record Item(
            @NotNull Long productId,
            @NotNull @Positive @Max(100_000) Integer quantity,
            @NotNull @PositiveOrZero @Digits(integer = 9, fraction = 3) BigDecimal unitCost,
            @Valid @Size(max = 1000) List<@NotNull ProductUnitRequest> units
    ) {

        public List<ProductUnitRequest> unitsOrEmpty() {
            return units == null ? List.of() : units;
        }
    }
}
