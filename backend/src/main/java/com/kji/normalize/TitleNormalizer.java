package com.kji.normalize;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class TitleNormalizer {

    private final List<Pattern> bracketPrefixes;
    private final List<String> protectedTerms;

    public TitleNormalizer(Lexicon lexicon) {
        this.bracketPrefixes = lexicon.titleBracketPrefixes().stream()
                .map(pattern -> Pattern.compile(pattern, Pattern.CASE_INSENSITIVE))
                .toList();
        this.protectedTerms = lexicon.seniorityExcluded().stream()
                .map(term -> term.toLowerCase(Locale.ROOT))
                .toList();
    }

    public String canonicalTitle(String rawTitle) {
        if (TextNormalizer.isBlank(rawTitle)) {
            return null;
        }
        return TextNormalizer.collapseWhitespace(
                TextNormalizer.compatibilityNormalize(rawTitle));
    }

    public String normalize(String rawTitle) {
        if (TextNormalizer.isBlank(rawTitle)) {
            return null;
        }
        String working = TextNormalizer.compatibilityNormalize(rawTitle);
        for (Pattern pattern : bracketPrefixes) {
            working = removeUnlessProtected(working, pattern);
        }
        String matched = TextNormalizer.normalizeForMatching(working);
        if (TextNormalizer.isBlank(matched)) {
            return TextNormalizer.normalizeForMatching(rawTitle);
        }
        return matched;
    }

    private String removeUnlessProtected(String value, Pattern pattern) {
        Matcher matcher = pattern.matcher(value);
        StringBuilder result = new StringBuilder();
        int cursor = 0;
        while (matcher.find()) {
            String segment = matcher.group();
            if (containsProtectedTerm(segment)) {
                continue;
            }
            result.append(value, cursor, matcher.start()).append(' ');
            cursor = matcher.end();
        }
        result.append(value, cursor, value.length());
        return result.toString();
    }

    private boolean containsProtectedTerm(String segment) {
        String lowered = segment.toLowerCase(Locale.ROOT);
        return protectedTerms.stream().anyMatch(lowered::contains);
    }
}
