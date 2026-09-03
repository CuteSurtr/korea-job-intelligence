package com.kji.scoring;

import com.kji.normalize.TermMatcher;
import com.kji.normalize.TextNormalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class CareerValueScorer {

    private static final double MAX_SCORE = 100.0d;
    private static final double MIN_SCORE = 0.0d;

    private final CareerValueWeights weights;

    public CareerValueScorer(CareerValueWeights weights) {
        this.weights = weights;
    }

    public ScoreResult score(ScoringInput input) {
        ScoreBreakdown breakdown = new ScoreBreakdown();
        Set<String> skillSlugs = input.skillSlugs();
        String haystack = haystack(input);

        for (CareerValueWeights.Component component : weights.components()) {
            Optional<String> evidence = matchComponent(component, input, skillSlugs);
            evidence.ifPresent(value ->
                    breakdown.add(component.key(), component.label(), component.weight(), value));
        }

        for (CareerValueWeights.Penalty penalty : weights.penalties()) {
            if (isCancelled(penalty, skillSlugs)) {
                continue;
            }
            Optional<String> hit = TermMatcher.matchedTerm(haystack, penalty.terms());
            hit.ifPresent(value ->
                    breakdown.add(penalty.key(), penalty.label(), penalty.weight(), value));
        }

        if (lacksDevelopmentSignal(input)) {
            CareerValueWeights.Penalty penalty = weights.noDevelopmentSignalPenalty();
            breakdown.add(penalty.key(), penalty.label(), penalty.weight(),
                    "no language or framework skill detected");
        }

        double raw = breakdown.total();
        double clamped = Math.max(MIN_SCORE, Math.min(MAX_SCORE, raw));
        double confidence = confidence(input);

        return new ScoreResult(clamped, confidence, weights.version(),
                breakdown.toJson(), breakdown.toExplanation());
    }

    private Optional<String> matchComponent(CareerValueWeights.Component component,
                                            ScoringInput input, Set<String> skillSlugs) {
        if (!component.roleFamilies().isEmpty()
                && input.roleFamily() != null
                && component.roleFamilies().contains(input.roleFamily())) {
            return Optional.of("role family " + input.roleFamily());
        }
        List<String> matched = new ArrayList<>();
        for (String skill : component.skills()) {
            if (skillSlugs.contains(skill)) {
                matched.add(skill);
            }
        }
        if (matched.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(String.join(", ", matched.subList(0, Math.min(4, matched.size()))));
    }

    private boolean isCancelled(CareerValueWeights.Penalty penalty, Set<String> skillSlugs) {
        return penalty.cancelledBySkills().stream().anyMatch(skillSlugs::contains);
    }

    private boolean lacksDevelopmentSignal(ScoringInput input) {
        if (input.roleFamily() == null
                || !weights.engineeringRoleFamilies().contains(input.roleFamily())) {
            return false;
        }
        Set<String> categories = input.skillCategories();
        return weights.developmentSignalSkillCategories().stream().noneMatch(categories::contains);
    }

    private double confidence(ScoringInput input) {
        CareerValueWeights.ThinExtraction thin = weights.thinExtraction();
        int descriptionLength = input.description() == null ? 0 : input.description().length();
        boolean thinDescription = descriptionLength < thin.minDescriptionLength();
        boolean thinSkills = input.skills().size() < thin.minSkillCount();
        if (thinDescription || thinSkills) {
            return thin.confidencePenalty();
        }
        return 0.90d;
    }

    private String haystack(ScoringInput input) {
        String title = input.title() == null ? "" : input.title();
        String description = TextNormalizer.truncate(
                input.description() == null ? "" : input.description(), 20_000);
        return title + "\n" + description;
    }
}
