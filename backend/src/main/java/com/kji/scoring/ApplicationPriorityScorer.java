package com.kji.scoring;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class ApplicationPriorityScorer {

    public static final String VERSION = "priority-1";

    private static final double CAREER_WEIGHT = 0.45d;
    private static final double FIT_WEIGHT = 0.45d;
    private static final long URGENT_DAYS = 7L;
    private static final long SOON_DAYS = 21L;

    private final Clock clock;

    public ApplicationPriorityScorer(Clock clock) {
        this.clock = clock;
    }

    public ScoreResult score(ScoringInput input, ScoreResult careerValue, ScoreResult candidateFit) {
        ScoreBreakdown breakdown = new ScoreBreakdown();

        breakdown.add("career_value", "SWE career value contribution",
                careerValue.score() * CAREER_WEIGHT,
                String.format(java.util.Locale.ROOT, "%.1f x %.2f", careerValue.score(), CAREER_WEIGHT));
        breakdown.add("candidate_fit", "Candidate fit contribution",
                candidateFit.score() * FIT_WEIGHT,
                String.format(java.util.Locale.ROOT, "%.1f x %.2f", candidateFit.score(), FIT_WEIGHT));

        addDeadlineUrgency(input, breakdown);
        addCompanyRisk(input, breakdown);

        double clamped = Math.max(0.0d, Math.min(100.0d, breakdown.total()));
        double confidence = Math.min(careerValue.confidence(), candidateFit.confidence());
        return new ScoreResult(clamped, confidence, VERSION,
                breakdown.toJson(), breakdown.toExplanation());
    }

    private void addDeadlineUrgency(ScoringInput input, ScoreBreakdown breakdown) {
        if (input.deadlineOpenEnded()) {
            breakdown.add("deadline_open", "Continuous hiring, no deadline pressure", 0.0d,
                    "open-ended");
            return;
        }
        if (input.deadlineAt() == null) {
            return;
        }
        Instant now = Instant.now(clock);
        if (input.deadlineAt().isBefore(now)) {
            breakdown.add("deadline_passed", "The stated deadline has passed", -20.0d,
                    input.deadlineAt().toString());
            return;
        }
        long days = Duration.between(now, input.deadlineAt()).toDays();
        if (days <= URGENT_DAYS) {
            breakdown.add("deadline_urgent", "Closes within a week", 10.0d, days + " days left");
        } else if (days <= SOON_DAYS) {
            breakdown.add("deadline_soon", "Closes within three weeks", 5.0d, days + " days left");
        }
    }

    private void addCompanyRisk(ScoringInput input, ScoreBreakdown breakdown) {
        String risk = input.companyRiskLevel();
        if (risk == null || "UNKNOWN".equals(risk)) {
            return;
        }
        switch (risk) {
            case "HIGH" -> breakdown.add("company_risk_high", "Company risk is high", -12.0d, risk);
            case "MODERATE" -> breakdown.add("company_risk_moderate", "Company risk is moderate",
                    -5.0d, risk);
            case "LOW" -> breakdown.add("company_risk_low", "Company risk is low", 3.0d, risk);
            default -> {
            }
        }
    }
}
