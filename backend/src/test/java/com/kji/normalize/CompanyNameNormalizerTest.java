package com.kji.normalize;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CompanyNameNormalizerTest {

    private CompanyNameNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new CompanyNameNormalizer(LexiconTestSupport.lexicon());
    }

    @Test
    @DisplayName("the two boards' renderings of one employer normalize to the same string")
    void collapsesKoreanLegalFormVariants() {
        String fromJobKorea = "㈜바바리퍼블리카";
        String fromSaramin = "(주)바바리퍼블리카";

        assertThat(normalizer.normalize(fromJobKorea))
                .isEqualTo(normalizer.normalize(fromSaramin))
                .isEqualTo("바바리퍼블리카");
    }

    @Test
    @DisplayName("a spelled-out Korean legal form is removed without a separating space")
    void stripsSpelledOutLegalForm() {
        assertThat(normalizer.normalize("주식회사카카오"))
                .isEqualTo("카카오");
    }

    @Test
    @DisplayName("English legal suffixes are removed only on a token boundary")
    void stripsEnglishSuffixesOnBoundaryOnly() {
        assertThat(normalizer.normalize("Coupang Corp.")).isEqualTo("coupang");
        assertThat(normalizer.normalize("Sendbird, Inc.")).isEqualTo("sendbird");
        assertThat(normalizer.normalize("Incheon Logistics")).isEqualTo("incheon logistics");
    }

    @Test
    @DisplayName("a name made only of a legal form does not normalize to nothing")
    void neverNormalizesToEmpty() {
        assertThat(normalizer.normalize("(주)")).isNotBlank();
        assertThat(normalizer.normalize("Inc.")).isNotBlank();
    }

    @Test
    @DisplayName("the display name keeps its original casing after the legal form is dropped")
    void preservesCasingInDisplayName() {
        assertThat(normalizer.canonicalDisplayName("VESSL AI, Inc.")).isEqualTo("VESSL AI,");
        assertThat(normalizer.canonicalDisplayName("Coupang Corp.")).isEqualTo("Coupang");
    }

    @Test
    @DisplayName("a missing company name normalizes to null rather than an empty key")
    void handlesMissingInput() {
        assertThat(normalizer.normalize(null)).isNull();
        assertThat(normalizer.normalize("   ")).isNull();
    }
}
