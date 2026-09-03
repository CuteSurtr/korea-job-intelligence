package com.kji.ingest;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ingestion_failures")
public class IngestionFailure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "search_run_id", nullable = false)
    private Long searchRunId;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Stage stage;

    @Column(name = "reason_code", nullable = false, length = 64)
    private String reasonCode;

    @Column(nullable = false)
    private String message;

    @Column(name = "external_id", length = 320)
    private String externalId;

    @Column(name = "source_url")
    private String sourceUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", columnDefinition = "jsonb")
    private String rawPayload;

    @Column(name = "raw_line")
    private String rawLine;

    @Column(name = "payload_hash", length = 64)
    private String payloadHash;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt = Instant.now();

    protected IngestionFailure() {
    }

    public IngestionFailure(Long searchRunId, Long sourceId, Stage stage, String reasonCode,
                            String message, String externalId, String sourceUrl,
                            String rawPayload, String rawLine, String payloadHash,
                            Instant occurredAt) {
        this.searchRunId = searchRunId;
        this.sourceId = sourceId;
        this.stage = stage;
        this.reasonCode = reasonCode;
        this.message = message;
        this.externalId = externalId;
        this.sourceUrl = sourceUrl;
        this.rawPayload = rawPayload;
        this.rawLine = rawLine;
        this.payloadHash = payloadHash;
        this.occurredAt = occurredAt;
    }

    public Long getId() {
        return id;
    }

    public Long getSearchRunId() {
        return searchRunId;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public Stage getStage() {
        return stage;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public String getMessage() {
        return message;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public String getRawLine() {
        return rawLine;
    }

    public String getPayloadHash() {
        return payloadHash;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public enum Stage {
        FETCH,
        PARSE,
        NORMALIZE,
        COMPANY_RESOLUTION,
        JOB_RESOLUTION,
        DEDUPLICATION,
        INTELLIGENCE,
        VERIFICATION,
        PERSIST,
        INDEX,
        RANK
    }
}
