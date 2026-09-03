package com.kji.web.dto;

import com.kji.scoring.JobScore;
import java.math.BigDecimal;
import java.time.Instant;

public record JobScoreResponse(
        String scoreKind,
        String profileCode,
        BigDecimal score,
        String scoreVersion,
        BigDecimal confidence,
        String componentScores,
        String explanation,
        Instant computedAt
) {

    public static JobScoreResponse from(JobScore score, String profileCode) {
        return new JobScoreResponse(
                score.getScoreKind().name(),
                profileCode,
                score.getScore(),
                score.getScoreVersion(),
                score.getConfidence(),
                score.getComponentScores(),
                score.getExplanation(),
                score.getComputedAt());
    }
}
