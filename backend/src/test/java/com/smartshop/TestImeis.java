package com.smartshop;

/**
 * Genere des IMEI valides (cle de Luhn) pour les tests.
 */
public final class TestImeis {

    private TestImeis() {
    }

    /** IMEI valide dont les 14 premiers chiffres sont derives de {@code seed}. */
    public static String valid(long seed) {
        String body = String.format("35%012d", seed);
        int sum = 0;
        for (int i = 0; i < 14; i++) {
            int digit = body.charAt(i) - '0';
            if (i % 2 == 1) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
        }
        return body + (10 - sum % 10) % 10;
    }
}
