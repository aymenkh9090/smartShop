package com.smartshop.sale.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaleCancelRequest(@NotBlank @Size(max = 200) String reason) {
}
