package com.kji.dedupe;

import com.kji.job.JobSource;

public record JobMatch(
        Long jobId,
        JobSource.MatchMethod method,
        double confidence,
        String evidence,
        ReviewCandidate reviewCandidate
) {

    public static JobMatch none() {
        return new JobMatch(null, JobSource.MatchMethod.NEW_JOB, 1.0d,
                "{\"rung\":\"none\"}", null);
    }

    public static JobMatch none(ReviewCandidate reviewCandidate) {
        return new JobMatch(null, JobSource.MatchMethod.NEW_JOB, 1.0d,
                "{\"rung\":\"none\"}", reviewCandidate);
    }

    public static JobMatch matched(Long jobId, JobSource.MatchMethod method, double confidence,
                                   String evidence) {
        return new JobMatch(jobId, method, confidence, evidence, null);
    }

    public boolean matched() {
        return jobId != null;
    }

    public record ReviewCandidate(Long jobId, JobSource.MatchMethod method, double confidence,
                                  String evidence) {
    }
}
