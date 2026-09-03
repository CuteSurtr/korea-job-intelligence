package com.kji.source.adapter.ats;

import com.fasterxml.jackson.databind.JsonNode;
import com.kji.normalize.TextNormalizer;
import com.kji.source.RawJobRecord;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class LeverBoardParser {

    public static final String SOURCE_CODE = "lever";

    public List<RawJobRecord> parse(JsonNode payload, String boardSlug, String companyName,
                                    Instant fetchedAt) {
        if (!payload.isArray()) {
            return List.of();
        }
        List<RawJobRecord> records = new ArrayList<>();
        for (JsonNode posting : payload) {
            RawJobRecord record = toRecord(posting, boardSlug, companyName, fetchedAt);
            if (record != null) {
                records.add(record);
            }
        }
        return records;
    }

    private RawJobRecord toRecord(JsonNode posting, String boardSlug, String companyName,
                                  Instant fetchedAt) {
        String externalId = text(posting, "id");
        if (externalId == null) {
            return null;
        }
        JsonNode categories = posting.path("categories");
        String description = joinPlainText(posting);

        Map<String, String> identifiers = new LinkedHashMap<>();
        identifiers.put("lever_board", boardSlug);

        return new RawJobRecord(
                SOURCE_CODE,
                boardSlug + ":" + externalId,
                text(posting, "hostedUrl"),
                text(posting, "hostedUrl"),
                fetchedAt,
                parseEpochMillis(text(posting, "createdAt")),
                text(posting, "text"),
                companyName != null ? companyName : boardSlug,
                text(categories, "location"),
                description,
                null,
                text(categories, "commitment"),
                null,
                null,
                null,
                text(posting, "workplaceType"),
                List.of(),
                identifiers,
                null,
                posting);
    }

    private String joinPlainText(JsonNode posting) {
        StringBuilder builder = new StringBuilder();
        appendIfPresent(builder, text(posting, "descriptionPlain"));
        appendIfPresent(builder, text(posting, "openingPlain"));
        appendIfPresent(builder, text(posting, "descriptionBodyPlain"));
        appendIfPresent(builder, text(posting, "additionalPlain"));
        String joined = builder.toString().trim();
        return joined.isEmpty() ? TextNormalizer.stripHtml(text(posting, "description")) : joined;
    }

    private void appendIfPresent(StringBuilder builder, String value) {
        if (value == null) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append("\n\n");
        }
        builder.append(value);
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String asText = value.isTextual() ? value.asText() : value.toString();
        return asText.isBlank() ? null : asText;
    }

    private Instant parseEpochMillis(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Instant.ofEpochMilli(Long.parseLong(value.trim()));
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
