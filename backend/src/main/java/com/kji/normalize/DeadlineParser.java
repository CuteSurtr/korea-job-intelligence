package com.kji.normalize;

import com.kji.normalize.Extracted.ExtractionMethod;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class DeadlineParser {

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    private static final Pattern FULL_DATE =
            Pattern.compile("(\\d{4})\\s*[.\\-/]\\s*(\\d{1,2})\\s*[.\\-/]\\s*(\\d{1,2})");
    private static final Pattern MONTH_DAY =
            Pattern.compile("(?<!\\d)(\\d{1,2})\\s*[./\\-]\\s*(\\d{1,2})(?!\\d)");
    private static final Pattern ISO_INSTANT = Pattern.compile(
            "\\d{4}-\\d{2}-\\d{2}T[\\d:.]+(?:Z|[+\\-]\\d{2}:?\\d{2})", Pattern.CASE_INSENSITIVE);

    private final List<String> openEndedTerms;
    private final Set<LocalDate> sentinelDates;
    private final List<String> todayTerms;
    private final List<String> tomorrowTerms;

    public DeadlineParser(Lexicon lexicon) {
        this.openEndedTerms = lowered(lexicon.openEndedDeadlineTerms());
        this.todayTerms = lowered(lexicon.deadlineTodayTerms());
        this.tomorrowTerms = lowered(lexicon.deadlineTomorrowTerms());
        this.sentinelDates = lexicon.deadlineSentinelDates().stream()
                .map(LocalDate::parse)
                .collect(Collectors.toUnmodifiableSet());
    }

    public Extracted<Deadline> parse(String rawDeadline, Instant reference) {
        if (TextNormalizer.isBlank(rawDeadline)) {
            return Extracted.unknown();
        }
        String normalized = TextNormalizer.compatibilityNormalize(rawDeadline)
                .toLowerCase(Locale.ROOT)
                .trim();

        Matcher isoMatcher = ISO_INSTANT.matcher(normalized);
        if (isoMatcher.find()) {
            try {
                return Extracted.of(Deadline.at(Instant.parse(isoMatcher.group().toUpperCase(Locale.ROOT))),
                        0.98d, rawDeadline, ExtractionMethod.SOURCE_STRUCTURED);
            } catch (DateTimeParseException ignored) {
                return Extracted.unknown(rawDeadline);
            }
        }

        Matcher fullDate = FULL_DATE.matcher(normalized);
        if (fullDate.find()) {
            LocalDate date = toLocalDate(fullDate.group(1), fullDate.group(2), fullDate.group(3));
            if (date == null) {
                return Extracted.unknown(rawDeadline);
            }
            if (sentinelDates.contains(date)) {
                return Extracted.of(Deadline.unbounded(), 0.90d, rawDeadline, ExtractionMethod.LEXICON);
            }
            return Extracted.of(Deadline.at(endOfDay(date)), 0.95d, fullDate.group(),
                    ExtractionMethod.PATTERN_MATCH);
        }

        if (containsAny(normalized, openEndedTerms)) {
            return Extracted.of(Deadline.unbounded(), 0.90d, rawDeadline, ExtractionMethod.LEXICON);
        }
        if (containsAny(normalized, todayTerms)) {
            return Extracted.of(Deadline.at(endOfDay(today(reference))), 0.85d, rawDeadline,
                    ExtractionMethod.LEXICON);
        }
        if (containsAny(normalized, tomorrowTerms)) {
            return Extracted.of(Deadline.at(endOfDay(today(reference).plusDays(1))), 0.85d, rawDeadline,
                    ExtractionMethod.LEXICON);
        }

        Matcher monthDay = MONTH_DAY.matcher(normalized);
        if (monthDay.find()) {
            LocalDate resolved = resolveMonthDay(monthDay.group(1), monthDay.group(2), reference);
            if (resolved == null) {
                return Extracted.unknown(rawDeadline);
            }
            return Extracted.of(Deadline.at(endOfDay(resolved)), 0.70d, monthDay.group(),
                    ExtractionMethod.PATTERN_MATCH);
        }

        return Extracted.unknown(rawDeadline);
    }

    private LocalDate resolveMonthDay(String monthText, String dayText, Instant reference) {
        LocalDate today = today(reference);
        LocalDate candidate = toLocalDate(String.valueOf(today.getYear()), monthText, dayText);
        if (candidate == null) {
            return null;
        }
        if (candidate.isBefore(today.minusDays(30))) {
            return candidate.plusYears(1);
        }
        return candidate;
    }

    private LocalDate toLocalDate(String yearText, String monthText, String dayText) {
        try {
            int year = Integer.parseInt(yearText.trim());
            int month = Integer.parseInt(monthText.trim());
            int day = Integer.parseInt(dayText.trim());
            return LocalDate.of(year, month, day);
        } catch (NumberFormatException | java.time.DateTimeException exception) {
            return null;
        }
    }

    private LocalDate today(Instant reference) {
        return ZonedDateTime.ofInstant(reference, KOREA).toLocalDate();
    }

    private Instant endOfDay(LocalDate date) {
        return date.atTime(LocalTime.MAX).atZone(KOREA).toInstant();
    }

    private boolean containsAny(String haystack, List<String> needles) {
        return needles.stream().anyMatch(haystack::contains);
    }

    private List<String> lowered(List<String> values) {
        return values.stream()
                .map(value -> TextNormalizer.compatibilityNormalize(value).toLowerCase(Locale.ROOT))
                .toList();
    }
}
