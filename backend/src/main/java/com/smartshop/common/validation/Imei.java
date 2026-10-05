package com.smartshop.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * IMEI valide : 15 chiffres dont le dernier est la cle de Luhn. Une valeur nulle ou vide est acceptee.
 */
@Documented
@Constraint(validatedBy = ImeiValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Imei {

    String message() default "IMEI invalide (15 chiffres avec clé de contrôle)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
