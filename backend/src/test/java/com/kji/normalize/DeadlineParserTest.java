package com.kji.normalize;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DeadlineParserTest {

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    private static final Instant REFERENCE =
            LocalDate.of(2026, 9, 3).atStartOfDay(KOREA).toInstant();

    private DeadlineParser parser;

    @BeforeEach
    void setUp() {
        parser = new DeadlineParser(LexiconTestSupport.lexicon());
    }

    @Test
    @DisplayName("the far-future sentinel is open-ended hiring, not a date in 2069")
    void treatsSentinelDateAsOpenEnded() {
        Extracted<Deadline> parsed = parser.parse("2069. 12. 31.", REFERENCE);

        assertThat(parsed.isKnown()).isTrue();
        assertThat(parsed.value().openEnded()).isTrue();
        assertThat(parsed.value().closesAt()).isNull();
    }

    @Test
    @DisplayName("a continuous-hiring phrase is open-ended")
    void treatsContinuousHiringAsOpenEnded() {
        assertThat(parser.parse("상시채용", REFERENCE).value().openEnded()).isTrue();
        assertThat(parser.parse("채용시", REFERENCE).value().openEnded()).isTrue();
    }

    @Test
    @DisplayName("a real dated deadline closes at the end of that day in Korea")
    void parsesFullDate() {
        Extracted<Deadline> parsed = parser.parse("2026. 10. 12.", REFERENCE);

        assertThat(parsed.value().openEnded()).isFalse();
        assertThat(parsed.value().closesAt())
                .isAfterOrEqualTo(LocalDate.of(2026, 10, 12).atStartOfDay(KOREA).toInstant())
                .isBefore(LocalDate.of(2026, 10, 13).atStartOfDay(KOREA).toInstant());
    }

    @Test
    @DisplayName("a month and day without a year resolves against the fetch date, at lower confidence")
    void parsesMonthDayRelativeToReference() {
        Extracted<Deadline> parsed = parser.parse("~ 10/06(월)", REFERENCE);

        assertThat(parsed.value().closesAt())
                .isAfterOrEqualTo(LocalDate.of(2026, 10, 6).atStartOfDay(KOREA).toInstant());
        assertThat(parsed.confidence()).isLessThan(0.8d);
    }

    @Test
    @DisplayName("closing today resolves to the end of the reference day")
    void parsesRelativeToday() {
        Extracted<Deadline> parsed = parser.parse("오늘마감", REFERENCE);

        assertThat(parsed.value().closesAt())
                .isBefore(LocalDate.of(2026, 9, 4).atStartOfDay(KOREA).toInstant());
    }

    @Test
    @DisplayName("an ISO timestamp from a structured source is taken at full confidence")
    void parsesIsoInstant() {
        Extracted<Deadline> parsed = parser.parse("2026-09-28T07:38:22.251+00:00", REFERENCE);

        assertThat(parsed.value().closesAt()).isEqualTo(Instant.parse("2026-09-28T07:38:22.251Z"));
        assertThat(parsed.method()).isEqualTo(Extracted.ExtractionMethod.SOURCE_STRUCTURED);
    }

    @Test
    @DisplayName("an unparseable deadline is unknown and never open-ended by default")
    void refusesUnparseableDeadline() {
        Extracted<Deadline> parsed = parser.parse("추후 공지", REFERENCE);

        assertThat(parsed.isKnown()).isFalse();
        assertThat(parser.parse(null, REFERENCE).isKnown()).isFalse();
    }
}
