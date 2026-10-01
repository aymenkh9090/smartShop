package com.smartshop.common.persistence;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SearchPatternsTest {

    @Test
    void escapesLikeWildcardsAndLowercases() {
        assertThat(SearchPatterns.containsPattern(" Câble_100% ")).isEqualTo("%câble\\_100\\%%");
        assertThat(SearchPatterns.containsPattern("a\\b")).isEqualTo("%a\\\\b%");
    }
}
