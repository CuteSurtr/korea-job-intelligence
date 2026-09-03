package com.kji.normalize;

import com.kji.normalize.Extracted.ExtractionMethod;
import com.kji.normalize.Lexicon.CompiledSalaryPattern;
import java.util.Locale;
import java.util.regex.Matcher;
import org.springframework.stereotype.Component;

@Component
public class SalaryParser {

    private static final long KRW_PER_10K = 10_000L;
    private static final long MIN_PLAUSIBLE_KRW_ANNUAL = 12_000_000L;
    private static final long MAX_PLAUSIBLE_KRW_ANNUAL = 1_000_000_000L;

    private final Lexicon lexicon;

    public SalaryParser(Lexicon lexicon) {
        this.lexicon = lexicon;
    }

    public Extracted<Salary> parse(String text) {
        if (TextNormalizer.isBlank(text)) {
            return Extracted.unknown();
        }
        String normalized = TextNormalizer.compatibilityNormalize(text).toLowerCase(Locale.ROOT);

        for (CompiledSalaryPattern candidate : lexicon.salaryPatterns()) {
            Matcher matcher = candidate.pattern().matcher(normalized);
            if (!matcher.find()) {
                continue;
            }
            Long first = amount(matcher.group(1), candidate.unit());
            if (first == null) {
                continue;
            }
            Long second = candidate.kind() == Lexicon.SalaryPatternKind.RANGE && matcher.groupCount() >= 2
                    ? amount(matcher.group(2), candidate.unit())
                    : null;

            if (candidate.kind() == Lexicon.SalaryPatternKind.RANGE) {
                if (second == null || second < first) {
                    continue;
                }
                return Extracted.of(new Salary(first, second, currencyOf(candidate.unit()), "YEAR"),
                        0.80d, matcher.group(), ExtractionMethod.PATTERN_MATCH);
            }
            return Extracted.of(new Salary(first, null, currencyOf(candidate.unit()), "YEAR"),
                    0.75d, matcher.group(), ExtractionMethod.PATTERN_MATCH);
        }
        return Extracted.unknown();
    }

    private Long amount(String captured, String unit) {
        if (captured == null) {
            return null;
        }
        long parsed;
        try {
            parsed = Long.parseLong(captured.replace(",", "").trim());
        } catch (NumberFormatException exception) {
            return null;
        }
        long value = "KRW_10K".equals(unit) ? parsed * KRW_PER_10K : parsed;
        if ("USD".equals(unit)) {
            return value;
        }
        if (value < MIN_PLAUSIBLE_KRW_ANNUAL || value > MAX_PLAUSIBLE_KRW_ANNUAL) {
            return null;
        }
        return value;
    }

    private String currencyOf(String unit) {
        return "USD".equals(unit) ? "USD" : "KRW";
    }

    public record Salary(Long min, Long max, String currency, String period) {
    }
}
