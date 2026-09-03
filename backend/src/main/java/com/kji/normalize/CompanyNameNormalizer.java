package com.kji.normalize;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class CompanyNameNormalizer {

    private static final Pattern ASCII_TOKEN = Pattern.compile("^[a-z0-9].*[a-z0-9]$|^[a-z0-9]$");

    private final List<LegalForm> legalForms;

    public CompanyNameNormalizer(Lexicon lexicon) {
        this.legalForms = lexicon.companyLegalForms().stream()
                .map(form -> TextNormalizer.compatibilityNormalize(form).toLowerCase(Locale.ROOT))
                .distinct()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .map(form -> new LegalForm(form, ASCII_TOKEN.matcher(form).matches()))
                .toList();
    }

    public String normalize(String rawName) {
        if (TextNormalizer.isBlank(rawName)) {
            return null;
        }
        String stripped = stripLegalForms(TextNormalizer.compatibilityNormalize(rawName));
        String matched = TextNormalizer.normalizeForMatching(stripped);
        if (TextNormalizer.isBlank(matched)) {
            return TextNormalizer.normalizeForMatching(rawName);
        }
        return matched;
    }

    public String canonicalDisplayName(String rawName) {
        if (TextNormalizer.isBlank(rawName)) {
            return null;
        }
        String normalized = TextNormalizer.compatibilityNormalize(rawName);
        String collapsed = TextNormalizer.collapseWhitespace(stripLegalForms(normalized));
        return TextNormalizer.isBlank(collapsed)
                ? TextNormalizer.collapseWhitespace(normalized)
                : collapsed;
    }

    private String stripLegalForms(String value) {
        String working = value;
        for (LegalForm form : legalForms) {
            working = removeOccurrences(working, form);
        }
        return working;
    }

    private String removeOccurrences(String value, LegalForm form) {
        StringBuilder result = new StringBuilder();
        String lowered = value.toLowerCase(Locale.ROOT);
        int cursor = 0;
        while (cursor < value.length()) {
            int index = lowered.indexOf(form.token(), cursor);
            if (index < 0) {
                result.append(value, cursor, value.length());
                break;
            }
            int end = index + form.token().length();
            if (form.boundaryRequired() && !isTokenBoundary(lowered, index, end)) {
                result.append(value, cursor, index + 1);
                cursor = index + 1;
                continue;
            }
            result.append(value, cursor, index).append(' ');
            cursor = end;
        }
        return result.toString();
    }

    private boolean isTokenBoundary(String lowered, int start, int end) {
        boolean leftOk = start == 0 || !Character.isLetterOrDigit(lowered.charAt(start - 1));
        boolean rightOk = end >= lowered.length() || !Character.isLetterOrDigit(lowered.charAt(end));
        return leftOk && rightOk;
    }

    private record LegalForm(String token, boolean boundaryRequired) {
    }
}
