package com.kji.ingest;

public record RecordIngestionResult(Outcome outcome, Long jobId, String externalKey) {

    public static RecordIngestionResult created(Long jobId, String externalKey) {
        return new RecordIngestionResult(Outcome.JOB_CREATED, jobId, externalKey);
    }

    public static RecordIngestionResult updated(Long jobId, String externalKey) {
        return new RecordIngestionResult(Outcome.JOB_UPDATED, jobId, externalKey);
    }

    public static RecordIngestionResult duplicate(Long jobId, String externalKey) {
        return new RecordIngestionResult(Outcome.DUPLICATE_MERGED, jobId, externalKey);
    }

    public static RecordIngestionResult failed(String externalKey) {
        return new RecordIngestionResult(Outcome.FAILED, null, externalKey);
    }

    public enum Outcome {
        JOB_CREATED,
        JOB_UPDATED,
        DUPLICATE_MERGED,
        FAILED
    }
}
