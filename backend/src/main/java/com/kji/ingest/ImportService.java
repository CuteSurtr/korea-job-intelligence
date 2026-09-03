package com.kji.ingest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kji.config.IngestionProperties;
import com.kji.source.RawJobRecord;
import com.kji.source.Source;
import com.kji.source.SourceException;
import com.kji.source.SourceRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ImportService {

    private final ObjectMapper objectMapper;
    private final IngestionPipeline pipeline;
    private final SourceRepository sourceRepository;
    private final IngestionProperties properties;

    public ImportService(ObjectMapper objectMapper,
                         IngestionPipeline pipeline,
                         SourceRepository sourceRepository,
                         IngestionProperties properties) {
        this.objectMapper = objectMapper;
        this.pipeline = pipeline;
        this.sourceRepository = sourceRepository;
        this.properties = properties;
    }

    public IngestionOutcome importNdjson(String sourceCode, InputStream body, String collector) {
        Source source = sourceRepository.findByCode(sourceCode)
                .orElseThrow(() -> new SourceException("Unknown source code " + sourceCode));

        ParsedLines parsed = parse(source, body, collector);
        return pipeline.runImport(sourceCode, parsed.records(), collector, parsed.malformed());
    }

    private ParsedLines parse(Source source, InputStream body, String collector) {
        List<RawJobRecord> records = new ArrayList<>();
        List<MalformedLine> malformed = new ArrayList<>();
        int lineNumber = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(body, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber > properties.importMaxLines()) {
                    throw new ImportTooLargeException(
                            "Import exceeds " + properties.importMaxLines() + " lines");
                }
                if (line.isBlank()) {
                    continue;
                }
                try {
                    RawJobRecord record = objectMapper.readValue(line, RawJobRecord.class);
                    if (record.sourceCode() != null && !record.sourceCode().equals(source.getCode())) {
                        malformed.add(new MalformedLine(line, "SOURCE_CODE_MISMATCH",
                                "Line declares source " + record.sourceCode()
                                        + " but was imported as " + source.getCode()));
                        continue;
                    }
                    records.add(withDefaults(record, source.getCode(), collector));
                } catch (IOException exception) {
                    malformed.add(new MalformedLine(line, "MALFORMED_JSON",
                            exception.getMessage() == null ? exception.toString() : exception.getMessage()));
                }
            }
        } catch (IOException exception) {
            throw new SourceException("Unable to read import stream", exception);
        }
        return new ParsedLines(records, malformed);
    }

    private RawJobRecord withDefaults(RawJobRecord record, String sourceCode, String collector) {
        if (record.sourceCode() != null && record.collector() != null) {
            return record;
        }
        return new RawJobRecord(
                record.sourceCode() == null ? sourceCode : record.sourceCode(),
                record.externalId(), record.sourceUrl(), record.originalApplyUrl(),
                record.fetchedAt(), record.postedAt(), record.rawTitle(), record.rawCompany(),
                record.rawLocation(), record.rawDescription(), record.rawRequirements(),
                record.rawEmploymentType(), record.rawExperience(), record.rawEducation(),
                record.rawDeadline(), record.rawRemotePolicy(), record.rawSkills(),
                record.companyIdentifiers(),
                record.collector() == null ? collector : record.collector(),
                record.rawPayload());
    }

    private record ParsedLines(List<RawJobRecord> records, List<MalformedLine> malformed) {
    }

    public static class ImportTooLargeException extends RuntimeException {

        public ImportTooLargeException(String message) {
            super(message);
        }
    }
}
