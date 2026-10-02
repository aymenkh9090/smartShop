package com.smartshop.aftersales.dto;

import com.smartshop.aftersales.entity.AfterSalesStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AfterSalesStatusRequest(@NotNull AfterSalesStatus status, @Size(max = 500) String comment) {
}
