package com.smartshop.aftersales.dto;

import jakarta.validation.constraints.NotNull;

public record TechnicianAssignRequest(@NotNull Long technicianId) {
}
