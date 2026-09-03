package com.kji.normalize;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TitleNormalizerTest {

    private TitleNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new TitleNormalizer(LexiconTestSupport.lexicon());
    }

    @Test
    @DisplayName("a bracketed brand prefix does not distinguish two renderings of one title")
    void stripsBracketedBrandPrefix() {
        assertThat(normalizer.normalize("[토스] Systems Engineer (GPU)"))
                .isEqualTo(normalizer.normalize("Systems Engineer (GPU)"));
    }

    @Test
    @DisplayName("a level token survives normalization so junior and senior never collapse")
    void preservesSeniorityTokens() {
        String junior = normalizer.normalize("Backend Engineer (Junior)");
        String senior = normalizer.normalize("Backend Engineer (Senior)");

        assertThat(junior).isNotEqualTo(senior);
        assertThat(junior).contains("junior");
        assertThat(senior).contains("senior");
    }

    @Test
    @DisplayName("a bracket containing a level token is kept rather than stripped as a brand")
    void keepsBracketedLevelTokens() {
        assertThat(normalizer.normalize("[Senior] Backend Engineer")).contains("senior");
    }

    @Test
    @DisplayName("the canonical title keeps the original text for display")
    void keepsCanonicalTitleReadable() {
        assertThat(normalizer.canonicalTitle("  [토스]   Systems Engineer (GPU)  "))
                .isEqualTo("[토스] Systems Engineer (GPU)");
    }

    @Test
    @DisplayName("a title consisting only of a bracketed prefix does not normalize to nothing")
    void neverNormalizesToEmpty() {
        assertThat(normalizer.normalize("[토스]")).isNotBlank();
    }
}
