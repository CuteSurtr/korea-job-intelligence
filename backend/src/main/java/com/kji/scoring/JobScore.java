package com.kji.scoring;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "job_scores")
public class JobScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Enumerated(EnumType.STRING)
    @Column(name = "score_kind", nullable = false, length = 32)
    private Kind scoreKind;

    @Column(name = "profile_id")
    private Long profileId;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal score;

    @Column(name = "score_version", nullable = false, length = 32)
    private String scoreVersion;

    @Column(nullable = false, precision = 4, scale = 3)
    private BigDecimal confidence;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "component_scores", nullable = false, columnDefinition = "jsonb")
    private String componentScores;

    @Column(nullable = false)
    private String explanation;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;

    protected JobScore() {
    }

    public JobScore(Long jobId, Kind scoreKind, Long profileId, BigDecimal score,
                    String scoreVersion, BigDecimal confidence, String componentScores,
                    String explanation, Instant computedAt) {
        this.jobId = jobId;
        this.scoreKind = scoreKind;
        this.profileId = profileId;
        this.score = score;
        this.scoreVersion = scoreVersion;
        this.confidence = confidence;
        this.componentScores = componentScores;
        this.explanation = explanation;
        this.computedAt = computedAt;
    }

    public void update(BigDecimal score, BigDecimal confidence, String componentScores,
                       String explanation, Instant computedAt) {
        this.score = score;
        this.confidence = confidence;
        this.componentScores = componentScores;
        this.explanation = explanation;
        this.computedAt = computedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getJobId() {
        return jobId;
    }

    public Kind getScoreKind() {
        return scoreKind;
    }

    public Long getProfileId() {
        return profileId;
    }

    public BigDecimal getScore() {
        return score;
    }

    public String getScoreVersion() {
        return scoreVersion;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public String getComponentScores() {
        return componentScores;
    }

    public String getExplanation() {
        return explanation;
    }

    public Instant getComputedAt() {
        return computedAt;
    }

    public enum Kind {
        CAREER_VALUE,
        CANDIDATE_FIT,
        APPLICATION_PRIORITY
    }
}
