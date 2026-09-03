package com.kji.scoring;

public record ScoreResult(
        double score,
        double confidence,
        String version,
        String componentJson,
        String explanation
) {
}
