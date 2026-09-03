package com.kji.normalize;

import com.kji.normalize.Extracted.ExtractionMethod;
import com.kji.normalize.Lexicon.CompiledExperiencePattern;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import org.springframework.stereotype.Component;

@Component
public class ExperienceParser {

    private final Lexicon lexicon;

    public ExperienceParser(Lexicon lexicon) {
        this.lexicon = lexicon;
    }

    public Extracted<ExperienceRequirement> parse(String rawExperience) {
        if (TextNormalizer.isBlank(rawExperience)) {
            return Extracted.unknown();
        }
        String normalized = TextNormalizer.compatibilityNormalize(rawExperience)
                .toLowerCase(Locale.ROOT)
                .trim();

        Extracted<ExperienceRequirement> numeric = parseNumeric(normalized, rawExperience);
        if (numeric.isKnown()) {
            return numeric;
        }

        if (containsAny(normalized, lexicon.experienceEitherNewGradOrExperienced())) {
            return Extracted.of(ExperienceRequirement.newGraduateOrExperienced(), 0.85d,
                    rawExperience, ExtractionMethod.LEXICON);
        }
        if (containsAny(normalized, lexicon.experienceIntern())) {
            return Extracted.of(ExperienceRequirement.intern(), 0.90d,
                    rawExperience, ExtractionMethod.LEXICON);
        }
        if (containsAny(normalized, lexicon.experienceNewGraduate())) {
            return Extracted.of(ExperienceRequirement.newGraduate(), 0.92d,
                    rawExperience, ExtractionMethod.LEXICON);
        }
        if (containsAny(normalized, lexicon.experienceAny())) {
            return Extracted.of(ExperienceRequirement.unconstrained(), 0.88d,
                    rawExperience, ExtractionMethod.LEXICON);
        }
        return Extracted.unknown(rawExperience);
    }

    private Extracted<ExperienceRequirement> parseNumeric(String normalized, String evidence) {
        for (CompiledExperiencePattern candidate : lexicon.experiencePatterns()) {
            Matcher matcher = candidate.pattern().matcher(normalized);
            if (!matcher.find()) {
                continue;
            }
            Integer first = parseYears(matcher.group(1));
            if (first == null) {
                return Extracted.unknown(evidence);
            }
            switch (candidate.kind()) {
                case RANGE -> {
                    Integer second = matcher.groupCount() >= 2 ? parseYears(matcher.group(2)) : null;
                    if (second == null || second < first) {
                        return Extracted.unknown(evidence);
                    }
                    return Extracted.of(ExperienceRequirement.range(first, second), 0.90d,
                            matcher.group(), ExtractionMethod.PATTERN_MATCH);
                }
                case MINIMUM -> {
                    return Extracted.of(ExperienceRequirement.minimum(first), 0.88d,
                            matcher.group(), ExtractionMethod.PATTERN_MATCH);
                }
                case MAXIMUM -> {
                    return Extracted.of(ExperienceRequirement.maximum(first), 0.85d,
                            matcher.group(), ExtractionMethod.PATTERN_MATCH);
                }
            }
        }
        return Extracted.unknown();
    }

    private Integer parseYears(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return null;
        }
        int years;
        try {
            years = Integer.parseInt(candidate.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
        if (years < 0 || years > lexicon.experienceImplausibleYears()) {
            return null;
        }
        return years;
    }

    private boolean containsAny(String haystack, List<String> needles) {
        for (String needle : needles) {
            String normalizedNeedle = TextNormalizer.compatibilityNormalize(needle)
                    .toLowerCase(Locale.ROOT);
            if (haystack.contains(normalizedNeedle)) {
                return true;
            }
        }
        return false;
    }
}
