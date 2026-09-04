package com.kji.normalize;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TermMatcherTest {

    @Test
    @DisplayName("a short Latin acronym does not match inside a longer English word")
    void latinTermsRespectWordBoundaries() {
        // Each of these was observed misclassifying a real posting before the boundary rule.
        assertThat(TermMatcher.occursAsTerm("data driven insights for teams", "hts")).isFalse();
        assertThat(TermMatcher.occursAsTerm("we are redefining the category", "defi")).isFalse();
        assertThat(TermMatcher.occursAsTerm("enterprise scale systems", "erp")).isFalse();
        assertThat(TermMatcher.occursAsTerm("handles many scenarios", "ios")).isFalse();
        assertThat(TermMatcher.occursAsTerm("furiosaai builds npus", "ios")).isFalse();
    }

    @Test
    @DisplayName("the same acronym still matches when it stands on its own")
    void latinTermsStillMatchAsWords() {
        assertThat(TermMatcher.occursAsTerm("hts and mts development", "hts")).isTrue();
        assertThat(TermMatcher.occursAsTerm("defi protocol engineer", "defi")).isTrue();
        assertThat(TermMatcher.occursAsTerm("erp integration", "erp")).isTrue();
        assertThat(TermMatcher.occursAsTerm("ios engineer", "ios")).isTrue();
        assertThat(TermMatcher.occursAsTerm("build for ios", "ios")).isTrue();
        // punctuation and slashes are boundaries, not word characters
        assertThat(TermMatcher.occursAsTerm("android/ios engineer", "ios")).isTrue();
        assertThat(TermMatcher.occursAsTerm("platform (ios)", "ios")).isTrue();
    }

    @Test
    @DisplayName("Korean keeps substring matching, because it is written without spaces")
    void koreanTermsStillMatchInsideCompounds() {
        // 백엔드개발자 is one word; requiring a boundary here would break every Korean term.
        assertThat(TermMatcher.occursAsTerm("백엔드개발자 모집", "백엔드")).isTrue();
        assertThat(TermMatcher.occursAsTerm("카카오페이증권 채용", "증권")).isTrue();
        assertThat(TermMatcher.occursAsTerm("토스뱅크 서버개발", "뱅크")).isTrue();
    }

    @Test
    @DisplayName("an ASCII term still matches when Korean text sits against it")
    void asciiTermsMatchAgainstHangul() {
        // Hangul is not a word character for this purpose, or "ios" would stop matching here.
        assertThat(TermMatcher.occursAsTerm("ios개발자", "ios")).isTrue();
        assertThat(TermMatcher.occursAsTerm("신입ios엔지니어", "ios")).isTrue();
    }

    @Test
    @DisplayName("hyphenated and spaced terms are unaffected")
    void punctuatedTermsStillWork() {
        assertThat(TermMatcher.occursAsTerm("back-end engineer", "back-end")).isTrue();
        assertThat(TermMatcher.occursAsTerm("a back end role", "back end")).isTrue();
    }

    @Test
    @DisplayName("firstMatch returns the first mapping that genuinely matches")
    void firstMatchUsesTheBoundaryRule() {
        List<Lexicon.TermMapping> mappings = List.of(
                new Lexicon.TermMapping(List.of("hts"), "SECURITIES", null),
                new Lexicon.TermMapping(List.of("platform"), "PLATFORM", null));

        Extracted<String> onlyInsideAWord =
                TermMatcher.firstMatch(mappings, "insights platform work", 0.7d);
        assertThat(onlyInsideAWord.value()).isEqualTo("PLATFORM");

        Extracted<String> standalone = TermMatcher.firstMatch(mappings, "hts platform", 0.7d);
        assertThat(standalone.value()).isEqualTo("SECURITIES");
    }

    @Test
    @DisplayName("matchedTerm and containsAny follow the same rule")
    void helpersAgree() {
        assertThat(TermMatcher.matchedTerm("insights", List.of("hts"))).isEmpty();
        assertThat(TermMatcher.containsAny("insights", List.of("hts"))).isFalse();
        assertThat(TermMatcher.matchedTerm("hts screen", List.of("hts"))).contains("hts");
        assertThat(TermMatcher.containsAny("hts screen", List.of("hts"))).isTrue();
    }

    @Test
    @DisplayName("blank input matches nothing")
    void blankIsSafe() {
        assertThat(TermMatcher.containsAny(null, List.of("hts"))).isFalse();
        assertThat(TermMatcher.containsAny("   ", List.of("hts"))).isFalse();
        assertThat(TermMatcher.occursAsTerm("", "hts")).isFalse();
    }
}
