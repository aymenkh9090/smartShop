package com.smartshop.common.validation;

import com.smartshop.TestImeis;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ImeiValidatorTest {

    private final ImeiValidator validator = new ImeiValidator();

    @Test
    void acceptsValidImeis() {
        assertThat(ImeiValidator.isValidImei("490154203237518")).isTrue();
        assertThat(ImeiValidator.isValidImei(TestImeis.valid(42))).isTrue();
    }

    @Test
    void rejectsWrongCheckDigitLengthOrCharacters() {
        assertThat(ImeiValidator.isValidImei("490154203237519")).isFalse();
        assertThat(ImeiValidator.isValidImei("49015420323751")).isFalse();
        assertThat(ImeiValidator.isValidImei("49015420323751A")).isFalse();
    }

    @Test
    void emptyValuesAreLeftToOtherConstraints() {
        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid("  ", null)).isTrue();
        assertThat(validator.isValid(" 490154203237518 ", null)).isTrue();
    }
}
