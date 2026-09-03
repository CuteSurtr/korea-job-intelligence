package com.kji.ingest;

import com.kji.common.Hashing;
import com.kji.source.RawJobRecord;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IngestionFailureWriter {

    private static final int MAX_MESSAGE_LENGTH = 4000;

    private final IngestionFailureRepository repository;
    private final Clock clock;

    public IngestionFailureWriter(IngestionFailureRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordRecordFailure(Long searchRunId, Long sourceId, IngestionFailure.Stage stage,
                                    String reasonCode, String message, RawJobRecord record) {
        String payload = record == null || record.rawPayload() == null
                ? null
                : record.rawPayload().toString();
        repository.save(new IngestionFailure(
                searchRunId,
                sourceId,
                stage,
                reasonCode,
                truncate(message),
                record == null ? null : record.externalId(),
                record == null ? null : record.sourceUrl(),
                payload,
                null,
                payload == null ? null : Hashing.sha256(payload),
                Instant.now(clock)));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLineFailure(Long searchRunId, Long sourceId, String reasonCode,
                                  String message, String rawLine) {
        repository.save(new IngestionFailure(
                searchRunId,
                sourceId,
                IngestionFailure.Stage.PARSE,
                reasonCode,
                truncate(message),
                null,
                null,
                null,
                truncate(rawLine),
                rawLine == null ? null : Hashing.sha256(rawLine),
                Instant.now(clock)));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFetchFailure(Long searchRunId, Long sourceId, String reasonCode, String message) {
        repository.save(new IngestionFailure(
                searchRunId,
                sourceId,
                IngestionFailure.Stage.FETCH,
                reasonCode,
                truncate(message),
                null,
                null,
                null,
                null,
                null,
                Instant.now(clock)));
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= MAX_MESSAGE_LENGTH ? value : value.substring(0, MAX_MESSAGE_LENGTH);
    }
}
