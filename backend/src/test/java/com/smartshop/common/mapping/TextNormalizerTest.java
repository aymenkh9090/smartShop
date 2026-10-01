package com.smartshop.common.mapping;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextNormalizerTest {

    @Test
    void trimToNull_turnsBlankIntoNull() {
        assertThat(TextNormalizer.trimToNull("  abc ")).isEqualTo("abc");
        assertThat(TextNormalizer.trimToNull("   ")).isNull();
        assertThat(TextNormalizer.trimToNull(null)).isNull();
    }

    @Test
    void upperLowerAndPhone() {
        assertThat(TextNormalizer.upper(" sku-1 ")).isEqualTo("SKU-1");
        assertThat(TextNormalizer.lower(" Ali@Mail.TN ")).isEqualTo("ali@mail.tn");
        assertThat(TextNormalizer.phone(" +216 22 333 444 ")).isEqualTo("+21622333444");
        assertThat(TextNormalizer.phone(" ")).isNull();
    }
}
