package com.smartshop.aftersales.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AfterSalesCancelRequest(@NotBlank @Size(max = 200) String reason) {
}
