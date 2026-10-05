package com.smartshop.common.credit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreditCancelRequest(@NotBlank @Size(max = 200) String reason) {
}
