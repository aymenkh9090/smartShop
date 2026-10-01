package com.smartshop.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ImeiValidator implements ConstraintValidator<Imei, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || value.isBlank() || isValidImei(value.trim());
    }

    /** Verifie le format (15 chiffres) et la cle de Luhn. */
    public static boolean isValidImei(String imei) {
        if (!imei.matches("\\d{15}")) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 15; i++) {
            int digit = imei.charAt(i) - '0';
            if (i % 2 == 1) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
        }
        return sum % 10 == 0;
    }
}
