package com.kji.dedupe;

import com.kji.job.JobSource;

public record JobMatch(Long jobId, JobSource.MatchMethod method, double confidence, String evidence) {

    public static JobMatch none() {
        return new JobMatch(null, JobSource.MatchMethod.NEW_JOB, 1.0d, "{\"rung\":\"none\"}");
    }

    public boolean matched() {
        return jobId != null;
    }
}
