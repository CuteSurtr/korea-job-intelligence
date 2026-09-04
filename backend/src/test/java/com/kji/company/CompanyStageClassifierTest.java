package com.kji.company;

import static org.assertj.core.api.Assertions.assertThat;

import com.kji.normalize.LexiconTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CompanyStageClassifierTest {

    private final CompanyStageClassifier classifier =
            new CompanyStageClassifier(LexiconTestSupport.lexicon());

    @Test
    @DisplayName("a large employer is recognised by name, wherever the posting came from")
    void largeIsRecognisedByName() {
        CompanyStageClassifier.Stage fromAggregator = classifier.classify("카카오페이", false);
        assertThat(fromAggregator.value()).isEqualTo("LARGE");
        assertThat(fromAggregator.evidence()).contains("카카오");

        // the board it was found on cannot make a conglomerate a hidden gem
        assertThat(classifier.classify("카카오페이", true).value()).isEqualTo("LARGE");
    }

    @Test
    @DisplayName("subsidiaries inherit the family name, which is how the prefix earns its place")
    void subsidiariesAreCaught() {
        for (String name : new String[] {
                "카카오모빌리티", "카카오페이증권", "네이버클라우드", "네이버파이낸셜", "라인플러스"}) {
            assertThat(classifier.classify(name, false).value())
                    .as("%s should be LARGE", name)
                    .isEqualTo("LARGE");
        }
    }

    @Test
    @DisplayName("an unknown employer on its own board is the population worth surfacing")
    void ownBoardMarksAnEmergingEmployer() {
        CompanyStageClassifier.Stage stage = classifier.classify("VESSL AI", true);

        assertThat(stage.value()).isEqualTo("EMERGING");
        assertThat(stage.evidence()).contains("own applicant tracking board");
    }

    @Test
    @DisplayName("an unknown employer seen only on an aggregator establishes nothing")
    void aggregatorOnlyStaysUnknown() {
        CompanyStageClassifier.Stage stage = classifier.classify("주식회사 이름없는회사", false);

        assertThat(stage.isKnown()).isFalse();
        assertThat(stage.value()).isNull();
        assertThat(stage.evidence()).isNull();
    }

    @Test
    @DisplayName("a blank or missing name is not treated as a match")
    void blankNamesAreSafe() {
        assertThat(classifier.classify(null, false).isKnown()).isFalse();
        assertThat(classifier.classify("", false).isKnown()).isFalse();
        // with no name, the board is still evidence of how the employer hires
        assertThat(classifier.classify(null, true).value()).isEqualTo("EMERGING");
    }

    @Test
    @DisplayName("no large-employer term is short enough to catch an unrelated company")
    void termsCannotCatchSmallCompanies() {
        // The list exists to exclude big names. A term that fired on ordinary words would mark
        // the very employers this is meant to surface, so every ASCII term must be specific.
        for (String term : LexiconTestSupport.lexicon().largeEmployers()) {
            boolean ascii = term.chars().allMatch(c -> c < 128);
            if (ascii) {
                assertThat(term.length())
                        .as("ASCII term %s is too short to be safe", term)
                        .isGreaterThanOrEqualTo(3);
            }
            assertThat(term).as("terms are matched lowercase").isEqualTo(term.toLowerCase());
        }
    }
}
