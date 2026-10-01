package com.smartshop.supplier.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SupplierRequest(
        @NotBlank @Size(max = 150) String name,

        @Pattern(regexp = "^\\s*\\+?[0-9 ]{8,20}$", message = "numero de telephone invalide")
        String phone,

        @Email @Size(max = 150) String email,
        @Size(max = 255) String address,
        @Size(max = 30) String taxId,
        @Size(max = 1000) String notes
) {
}
