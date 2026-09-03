package com.kji.source.adapter.ats;

import com.fasterxml.jackson.databind.JsonNode;
import com.kji.normalize.TextNormalizer;
import com.kji.source.RawJobRecord;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AshbyBoardParser {

    public static final String SOURCE_CODE = "ashby";

    public List<RawJobRecord> parse(JsonNode payload, String boardSlug, String companyName, Instant fetchedAt) {
        JsonNode jobs = payload.path("jobs");
        if (!jobs.isArray()) {
            return List.of();
        }
        List<RawJobRecord> records = new ArrayList<>();
        for (JsonNode job : jobs) {
            if (job.path("isListed").isBoolean() && !job.path("isListed").asBoolean()) {
                continue;
            }
            RawJobRecord record = toRecord(job, boardSlug, companyName, fetchedAt);
            if (record != null) {
                records.add(record);
            }
        }
        return records;
    }

    private RawJobRecord toRecord(JsonNode job, String boardSlug, String companyName, Instant fetchedAt) {
        String externalId = text(job, "id");
        if (externalId == null) {
            return null;
        }
        String jobUrl = text(job, "jobUrl");
        String applyUrl = text(job, "applyUrl");
        String description = text(job, "descriptionPlain");
        if (description == null) {
            description = TextNormalizer.stripHtml(text(job, "descriptionHtml"));
        }

        Map<String, String> identifiers = new LinkedHashMap<>();
        identifiers.put("ashby_board", boardSlug);

        return new RawJobRecord(
                SOURCE_CODE,
                boardSlug + ":" + externalId,
                jobUrl,
                applyUrl != null ? applyUrl : jobUrl,
                fetchedAt,
                parseInstant(text(job, "publishedAt")),
                text(job, "title"),
                companyName != null ? companyName : boardSlug,
                text(job, "location"),
                description,
                null,
                text(job, "employmentType"),
                null,
                null,
                null,
                text(job, "workplaceType"),
                List.of(),
                identifiers,
                null,
                job);
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String asText = value.isTextual() ? value.asText() : value.toString();
        return asText.isBlank() ? null : asText;
    }

    private Instant parseInstant(String value) {
        if (value == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (DateTimeParseException exception) {
            return null;
        }
    }
}
