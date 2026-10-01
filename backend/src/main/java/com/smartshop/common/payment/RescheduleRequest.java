package com.smartshop.common.payment;

import jakarta.validation.constraints.FutureOrPresent;

import java.time.LocalDate;

/**
 * @param dueDate nouvelle echeance ; null supprime l'echeance
 */
public record RescheduleRequest(@FutureOrPresent LocalDate dueDate) {
}
