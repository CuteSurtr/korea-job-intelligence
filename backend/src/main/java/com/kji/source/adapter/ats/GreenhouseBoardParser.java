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
public class GreenhouseBoardParser {

    public static final String SOURCE_CODE = "greenhouse";

    public List<RawJobRecord> parse(JsonNode payload, String boardToken, Instant fetchedAt) {
        JsonNode jobs = payload.path("jobs");
        if (!jobs.isArray()) {
            return List.of();
        }
        List<RawJobRecord> records = new ArrayList<>();
        for (JsonNode job : jobs) {
            RawJobRecord record = toRecord(job, boardToken, fetchedAt);
            if (record != null) {
                records.add(record);
            }
        }
        return records;
    }

    private RawJobRecord toRecord(JsonNode job, String boardToken, Instant fetchedAt) {
        String externalId = text(job, "id");
        if (externalId == null) {
            return null;
        }
        String applyUrl = text(job, "absolute_url");
        String companyName = text(job, "company_name");
        if (companyName == null) {
            companyName = boardToken;
        }
        String description = TextNormalizer.stripHtml(text(job, "content"));

        Map<String, String> identifiers = new LinkedHashMap<>();
        identifiers.put("greenhouse_board", boardToken);

        return new RawJobRecord(
                SOURCE_CODE,
                boardToken + ":" + externalId,
                applyUrl,
                applyUrl,
                fetchedAt,
                parseInstant(text(job, "first_published")),
                text(job, "title"),
                companyName,
                job.path("location").path("name").isMissingNode()
                        ? null
                        : text(job.path("location"), "name"),
                description,
                null,
                null,
                null,
                null,
                text(job, "application_deadline"),
                null,
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
