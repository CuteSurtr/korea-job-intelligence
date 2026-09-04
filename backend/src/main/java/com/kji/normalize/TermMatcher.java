package com.kji.normalize;

import com.kji.normalize.Extracted.ExtractionMethod;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class TermMatcher {

    private TermMatcher() {
    }

    /**
     * Whether a term occurs in text, as a term rather than as a fragment of a longer word.
     *
     * <p>A plain substring test is right for Korean, which is written without spaces between a
     * noun and its modifiers: 백엔드개발자 has to match 백엔드. It is wrong for short Latin
     * acronyms, which hide inside ordinary English words — {@code hts} sits in "insights",
     * {@code erp} in "enterprise", {@code defi} in "redefining", {@code ios} in "scenarios".
     * Each of those was observed classifying a real posting into a sector or role family it has
     * nothing to do with.
     *
     * <p>So the rule depends on the term: one written entirely in ASCII must not be flanked by
     * another ASCII letter or digit, and anything containing non-ASCII characters keeps the
     * substring behaviour that Korean needs. A term like {@code back-end} is unaffected, because
     * the hyphen is not a letter and the boundary check only looks at what surrounds the match.
     */
    static boolean occursAsTerm(String haystack, String term) {
        if (term.isEmpty()) {
            return false;
        }
        if (!isAscii(term)) {
            return haystack.contains(term);
        }
        int from = 0;
        while (true) {
            int at = haystack.indexOf(term, from);
            if (at < 0) {
                return false;
            }
            boolean openBefore = at == 0 || !isWordCharacter(haystack.charAt(at - 1));
            int after = at + term.length();
            boolean openAfter =
                    after >= haystack.length() || !isWordCharacter(haystack.charAt(after));
            if (openBefore && openAfter) {
                return true;
            }
            from = at + 1;
        }
    }

    private static boolean isAscii(String term) {
        for (int i = 0; i < term.length(); i++) {
            if (term.charAt(i) > 127) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether a character would make an adjacent match part of a longer word.
     *
     * <p>Only ASCII letters and digits count. A Hangul syllable must not, or an ASCII term would
     * stop matching inside Korean text where it legitimately appears.
     */
    private static boolean isWordCharacter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9');
    }

    public static Extracted<String> firstMatch(List<Lexicon.TermMapping> mappings, String haystack,
                                               double confidence) {
        if (TextNormalizer.isBlank(haystack)) {
            return Extracted.unknown();
        }
        String lowered = TextNormalizer.compatibilityNormalize(haystack).toLowerCase(Locale.ROOT);
        for (Lexicon.TermMapping mapping : mappings) {
            Optional<String> hit = mapping.terms().stream()
                    .map(term -> TextNormalizer.compatibilityNormalize(term).toLowerCase(Locale.ROOT))
                    .filter(term -> occursAsTerm(lowered, term))
                    .findFirst();
            if (hit.isPresent()) {
                return Extracted.of(mapping.resolved(), confidence, hit.get(), ExtractionMethod.LEXICON);
            }
        }
        return Extracted.unknown();
    }

    public static boolean containsAny(String haystack, List<String> terms) {
        return matchedTerm(haystack, terms).isPresent();
    }

    public static Optional<String> matchedTerm(String haystack, List<String> terms) {
        if (TextNormalizer.isBlank(haystack)) {
            return Optional.empty();
        }
        String lowered = TextNormalizer.compatibilityNormalize(haystack).toLowerCase(Locale.ROOT);
        return terms.stream()
                .map(term -> TextNormalizer.compatibilityNormalize(term).toLowerCase(Locale.ROOT))
                .filter(term -> occursAsTerm(lowered, term))
                .findFirst();
    }
}
