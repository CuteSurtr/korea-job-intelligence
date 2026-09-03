package com.kji.scoring;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class CandidateFitScorer {

    public static final String VERSION = "fit-1";

    private static final double SENIORITY_WEIGHT = 40.0d;
    private static final double ROLE_FAMILY_WEIGHT = 20.0d;
    private static final double SKILL_WEIGHT = 25.0d;
    private static final double LOCATION_WEIGHT = 10.0d;
    private static final double REMOTE_WEIGHT = 5.0d;

    public ScoreResult score(ScoringInput input, CandidateProfileData profile) {
        ScoreBreakdown breakdown = new ScoreBreakdown();

        scoreSeniority(input, profile, breakdown);
        scoreRoleFamily(input, profile, breakdown);
        scoreSkills(input, profile, breakdown);
        scoreLocation(input, profile, breakdown);
        scoreRemotePolicy(input, profile, breakdown);

        double clamped = Math.max(0.0d, Math.min(100.0d, breakdown.total()));
        return new ScoreResult(clamped, confidence(input), VERSION,
                breakdown.toJson(), breakdown.toExplanation());
    }

    private void scoreSeniority(ScoringInput input, CandidateProfileData profile,
                                ScoreBreakdown breakdown) {
        String bucket = input.seniorityBucket();
        if (bucket == null) {
            breakdown.add("seniority_unknown", "Seniority could not be established", 0.0d,
                    "no seniority evidence");
            return;
        }
        if (profile.target().seniorityBuckets().contains(bucket)) {
            double points = switch (bucket) {
                case "A" -> SENIORITY_WEIGHT;
                case "B" -> SENIORITY_WEIGHT * 0.85d;
                case "C" -> SENIORITY_WEIGHT * 0.55d;
                default -> SENIORITY_WEIGHT * 0.3d;
            };
            breakdown.add("seniority_match", "Seniority " + bucket + " is within the target range",
                    points, "bucket " + bucket);
            return;
        }
        double penalty = "X".equals(bucket) ? -30.0d : -15.0d;
        breakdown.add("seniority_mismatch", "Seniority " + bucket + " is outside the target range",
                penalty, "bucket " + bucket);
    }

    private void scoreRoleFamily(ScoringInput input, CandidateProfileData profile,
                                 ScoreBreakdown breakdown) {
        String roleFamily = input.roleFamily();
        if (roleFamily == null) {
            return;
        }
        if (profile.target().roleFamilies().contains(roleFamily)) {
            breakdown.add("role_family_match", "Role family " + roleFamily + " is a stated target",
                    ROLE_FAMILY_WEIGHT, roleFamily);
        } else {
            breakdown.add("role_family_other", "Role family " + roleFamily + " is outside the target",
                    ROLE_FAMILY_WEIGHT * 0.25d, roleFamily);
        }
    }

    private void scoreSkills(ScoringInput input, CandidateProfileData profile,
                             ScoreBreakdown breakdown) {
        Set<String> jobSkills = input.skillSlugs();
        if (jobSkills.isEmpty()) {
            return;
        }
        List<String> strong = overlap(jobSkills, profile.skills().strong());
        List<String> working = overlap(jobSkills, profile.skills().working());
        List<String> interest = overlap(jobSkills, profile.skills().interest());

        double covered = strong.size() * 1.0d + working.size() * 0.7d + interest.size() * 0.4d;
        double points = Math.min(SKILL_WEIGHT, covered * (SKILL_WEIGHT / 6.0d));
        if (points > 0) {
            breakdown.add("skill_overlap", "Overlap with the candidate's stack", points,
                    describeOverlap(strong, working, interest));
        }

        int unmatched = jobSkills.size() - strong.size() - working.size() - interest.size();
        if (unmatched > 6) {
            breakdown.add("skill_gap", "Many required skills are outside the candidate's stack",
                    -8.0d, unmatched + " unmatched skills");
        }
    }

    private void scoreLocation(ScoringInput input, CandidateProfileData profile,
                               ScoreBreakdown breakdown) {
        String country = input.locationCountry();
        if (country == null || profile.preferences().locationCountries().isEmpty()) {
            return;
        }
        if (profile.preferences().locationCountries().contains(country)) {
            breakdown.add("location_match", "Location is in a preferred country",
                    LOCATION_WEIGHT, country);
        } else {
            breakdown.add("location_mismatch", "Location is outside the preferred countries",
                    -10.0d, country);
        }
    }

    private void scoreRemotePolicy(ScoringInput input, CandidateProfileData profile,
                                   ScoreBreakdown breakdown) {
        String policy = input.remotePolicy();
        if (policy == null || "UNKNOWN".equals(policy)
                || profile.preferences().remotePolicies().isEmpty()) {
            return;
        }
        if (profile.preferences().remotePolicies().contains(policy)) {
            breakdown.add("remote_policy_match", "Work format " + policy + " is acceptable",
                    REMOTE_WEIGHT, policy);
        }
    }

    private List<String> overlap(Set<String> jobSkills, List<String> candidateSkills) {
        List<String> matched = new ArrayList<>();
        for (String skill : candidateSkills) {
            if (jobSkills.contains(skill)) {
                matched.add(skill);
            }
        }
        return matched;
    }

    private String describeOverlap(List<String> strong, List<String> working, List<String> interest) {
        List<String> parts = new ArrayList<>();
        if (!strong.isEmpty()) {
            parts.add("strong: " + String.join(", ", strong));
        }
        if (!working.isEmpty()) {
            parts.add("working: " + String.join(", ", working));
        }
        if (!interest.isEmpty()) {
            parts.add("interest: " + String.join(", ", interest));
        }
        return String.join("; ", parts);
    }

    private double confidence(ScoringInput input) {
        if (input.seniorityBucket() == null || input.skills().isEmpty()) {
            return 0.45d;
        }
        return 0.85d;
    }
}
