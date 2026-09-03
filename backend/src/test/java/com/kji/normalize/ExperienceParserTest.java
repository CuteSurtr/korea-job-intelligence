package com.kji.normalize;

import static org.assertj.core.api.Assertions.assertThat;

import com.kji.normalize.ExperienceRequirement.Kind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExperienceParserTest {

    private ExperienceParser parser;

    @BeforeEach
    void setUp() {
        parser = new ExperienceParser(LexiconTestSupport.lexicon());
    }

    @Test
    @DisplayName("a new-graduate posting reads as zero years, not as unknown")
    void parsesNewGraduate() {
        Extracted<ExperienceRequirement> parsed = parser.parse("신입");

        assertThat(parsed.isKnown()).isTrue();
        assertThat(parsed.value().kind()).isEqualTo(Kind.NEW_GRADUATE);
        assertThat(parsed.value().yearsMin()).isZero();
        assertThat(parsed.value().yearsMax()).isNull();
        assertThat(parsed.confidence()).isGreaterThan(0.9d);
    }

    @Test
    @DisplayName("an unconstrained posting carries no year bounds at all")
    void parsesUnconstrained() {
        Extracted<ExperienceRequirement> parsed = parser.parse("경력무관");

        assertThat(parsed.value().kind()).isEqualTo(Kind.ANY);
        assertThat(parsed.value().yearsMin()).isNull();
        assertThat(parsed.value().yearsMax()).isNull();
    }

    @Test
    @DisplayName("the two boards' renderings of the same requirement produce the same bound")
    void parsesEquivalentMinimumsFromBothBoards() {
        Extracted<ExperienceRequirement> spelled = parser.parse("경력 3년 이상");
        Extracted<ExperienceRequirement> arrow = parser.parse("경력3년↑");

        assertThat(spelled.value().yearsMin()).isEqualTo(3);
        assertThat(arrow.value().yearsMin()).isEqualTo(3);
        assertThat(spelled.value().yearsMax()).isNull();
    }

    @Test
    @DisplayName("a stated range keeps both bounds")
    void parsesRange() {
        Extracted<ExperienceRequirement> parsed = parser.parse("경력 5~12년");

        assertThat(parsed.value().yearsMin()).isEqualTo(5);
        assertThat(parsed.value().yearsMax()).isEqualTo(12);
    }

    @Test
    @DisplayName("an upper bound is not mistaken for a lower bound")
    void parsesMaximum() {
        Extracted<ExperienceRequirement> parsed = parser.parse("경력3년↓");

        assertThat(parsed.value().yearsMin()).isNull();
        assertThat(parsed.value().yearsMax()).isEqualTo(3);
    }

    @Test
    @DisplayName("an implausible year count is answered as unknown, with the source text kept")
    void refusesImplausibleYears() {
        Extracted<ExperienceRequirement> parsed = parser.parse("경력 100년 이상");

        assertThat(parsed.isKnown()).isFalse();
        assertThat(parsed.confidence()).isZero();
        assertThat(parsed.evidence()).isEqualTo("경력 100년 이상");
    }

    @Test
    @DisplayName("an English minimum is read the same way as the Korean one")
    void parsesEnglishMinimum() {
        assertThat(parser.parse("5+ years of experience").value().yearsMin()).isEqualTo(5);
        assertThat(parser.parse("At least 2 years").value().yearsMin()).isEqualTo(2);
    }

    @Test
    @DisplayName("a posting open to both keeps a zero floor rather than claiming seniority")
    void parsesNewGraduateOrExperienced() {
        Extracted<ExperienceRequirement> parsed = parser.parse("신입/경력");

        assertThat(parsed.value().kind()).isEqualTo(Kind.NEW_GRADUATE_OR_EXPERIENCED);
        assertThat(parsed.value().yearsMin()).isZero();
    }

    @Test
    @DisplayName("an absent field is unknown rather than zero")
    void treatsMissingInputAsUnknown() {
        assertThat(parser.parse(null).isKnown()).isFalse();
        assertThat(parser.parse("").isKnown()).isFalse();
        assertThat(parser.parse("협의").isKnown()).isFalse();
    }
}
