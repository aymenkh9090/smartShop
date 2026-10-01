package com.smartshop.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,

        @NotBlank
        @Pattern(regexp = "^\\s*\\+?[0-9 ]{8,20}$", message = "numero de telephone invalide")
        String phone,

        @Email @Size(max = 150) String email,
        @Size(max = 255) String address,

        @Pattern(regexp = "^\\s*[A-Za-z0-9]{6,20}\\s*$", message = "CIN invalide")
        String cin,

        @Size(max = 1000) String notes
) {
}
