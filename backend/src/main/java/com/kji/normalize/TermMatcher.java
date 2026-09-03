package com.kji.normalize;

import com.kji.normalize.Extracted.ExtractionMethod;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class TermMatcher {

    private TermMatcher() {
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
                    .filter(lowered::contains)
                    .findFirst();
            if (hit.isPresent()) {
                return Extracted.of(mapping.resolved(), confidence, hit.get(), ExtractionMethod.LEXICON);
            }
        }
        return Extracted.unknown();
    }

    public static boolean containsAny(String haystack, List<String> terms) {
        if (TextNormalizer.isBlank(haystack)) {
            return false;
        }
        String lowered = TextNormalizer.compatibilityNormalize(haystack).toLowerCase(Locale.ROOT);
        return terms.stream()
                .map(term -> TextNormalizer.compatibilityNormalize(term).toLowerCase(Locale.ROOT))
                .anyMatch(lowered::contains);
    }

    public static Optional<String> matchedTerm(String haystack, List<String> terms) {
        if (TextNormalizer.isBlank(haystack)) {
            return Optional.empty();
        }
        String lowered = TextNormalizer.compatibilityNormalize(haystack).toLowerCase(Locale.ROOT);
        return terms.stream()
                .map(term -> TextNormalizer.compatibilityNormalize(term).toLowerCase(Locale.ROOT))
                .filter(lowered::contains)
                .findFirst();
    }
}
