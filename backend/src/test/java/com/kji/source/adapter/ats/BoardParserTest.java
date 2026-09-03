package com.kji.source.adapter.ats;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kji.source.RawJobRecord;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BoardParserTest {

    private static final Instant FETCHED_AT = Instant.parse("2026-09-03T00:00:00Z");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    @DisplayName("Greenhouse postings become records carrying the board token in the external id")
    void parsesGreenhouseBoard() throws IOException {
        List<RawJobRecord> records = new GreenhouseBoardParser()
                .parse(fixture("fixtures/greenhouse/coupang-board.json"), "coupang", FETCHED_AT);

        assertThat(records).isNotEmpty();
        RawJobRecord record = records.stream()
                .filter(candidate -> candidate.rawTitle().contains("Staff Backend Engineer"))
                .findFirst()
                .orElseThrow();

        assertThat(record.sourceCode()).isEqualTo("greenhouse");
        assertThat(record.externalId()).isEqualTo("coupang:8168878");
        assertThat(record.rawLocation()).contains("Seoul");
        assertThat(record.originalApplyUrl()).contains("gh_jid=8168878");
        assertThat(record.companyIdentifiers()).containsEntry("greenhouse_board", "coupang");
        assertThat(record.fetchedAt()).isEqualTo(FETCHED_AT);
    }

    @Test
    @DisplayName("Greenhouse HTML-escaped content is unescaped and stripped to readable text")
    void unescapesGreenhouseContent() throws IOException {
        List<RawJobRecord> records = new GreenhouseBoardParser()
                .parse(fixture("fixtures/greenhouse/coupang-board.json"), "coupang", FETCHED_AT);

        RawJobRecord record = records.get(0);
        assertThat(record.rawDescription())
                .isNotBlank()
                .doesNotContain("&lt;p&gt;")
                .doesNotContain("<p>")
                .doesNotContain("&amp;nbsp;");
    }

    @Test
    @DisplayName("Ashby postings keep the plain-text description and the workplace type")
    void parsesAshbyBoard() throws IOException {
        List<RawJobRecord> records = new AshbyBoardParser()
                .parse(fixture("fixtures/ashby/vessl-ai-board.json"), "vessl-ai", "VESSL AI", FETCHED_AT);

        RawJobRecord junior = records.stream()
                .filter(candidate -> candidate.rawTitle().equals("Backend Engineer (Junior)"))
                .findFirst()
                .orElseThrow();

        assertThat(junior.sourceCode()).isEqualTo("ashby");
        assertThat(junior.externalId())
                .isEqualTo("vessl-ai:8673f35a-c56f-4250-92be-d2fd1eb6e48f");
        assertThat(junior.rawCompany()).isEqualTo("VESSL AI");
        assertThat(junior.rawLocation()).isEqualTo("Seoul");
        assertThat(junior.rawEmploymentType()).isEqualTo("FullTime");
        assertThat(junior.rawRemotePolicy()).isEqualTo("Hybrid");
        assertThat(junior.rawDescription()).isNotBlank().doesNotContain("<h2>");
        assertThat(junior.postedAt()).isNotNull();
    }

    @Test
    @DisplayName("a junior and a senior posting at one company stay distinguishable records")
    void keepsJuniorAndSeniorDistinct() throws IOException {
        List<RawJobRecord> records = new AshbyBoardParser()
                .parse(fixture("fixtures/ashby/vessl-ai-board.json"), "vessl-ai", "VESSL AI", FETCHED_AT);

        List<String> backendIds = records.stream()
                .filter(record -> record.rawTitle().startsWith("Backend Engineer"))
                .map(RawJobRecord::externalId)
                .toList();

        assertThat(backendIds).hasSize(2).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("the same posting hashes to the same content hash across two fetches")
    void contentHashIsStableAcrossFetches() throws IOException {
        RawJobRecord first = new AshbyBoardParser()
                .parse(fixture("fixtures/ashby/vessl-ai-board.json"), "vessl-ai", "VESSL AI",
                        Instant.parse("2026-09-01T00:00:00Z"))
                .get(0);
        RawJobRecord second = new AshbyBoardParser()
                .parse(fixture("fixtures/ashby/vessl-ai-board.json"), "vessl-ai", "VESSL AI",
                        Instant.parse("2026-09-05T00:00:00Z"))
                .get(0);

        assertThat(first.contentHash()).isEqualTo(second.contentHash());
    }

    @Test
    @DisplayName("Lever postings keep the plain-text body and the hosted URL")
    void parsesLeverBoard() throws IOException {
        List<RawJobRecord> records = new LeverBoardParser()
                .parse(fixture("fixtures/lever/aleph-board.json"), "aleph", "Aleph", FETCHED_AT);

        assertThat(records).isNotEmpty();
        RawJobRecord record = records.get(0);

        assertThat(record.sourceCode()).isEqualTo("lever");
        assertThat(record.externalId()).startsWith("aleph:");
        assertThat(record.rawCompany()).isEqualTo("Aleph");
        assertThat(record.rawLocation()).contains("Seoul");
        assertThat(record.rawEmploymentType()).isNotBlank();
        assertThat(record.sourceUrl()).startsWith("https://jobs.lever.co/aleph/");
        assertThat(record.rawDescription()).isNotBlank().doesNotContain("<div");
        assertThat(record.postedAt()).isNotNull();
    }

    @Test
    @DisplayName("an empty payload yields no records rather than throwing")
    void toleratesEmptyPayload() throws IOException {
        JsonNode empty = MAPPER.readTree("{}");

        assertThat(new GreenhouseBoardParser().parse(empty, "coupang", FETCHED_AT)).isEmpty();
        assertThat(new AshbyBoardParser().parse(empty, "vessl-ai", null, FETCHED_AT)).isEmpty();
        assertThat(new LeverBoardParser().parse(empty, "aleph", null, FETCHED_AT)).isEmpty();
    }

    private JsonNode fixture(String path) throws IOException {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing fixture " + path);
            }
            return MAPPER.readTree(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }
    }
}
