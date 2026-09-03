package com.kji.intelligence;

import com.kji.normalize.Extracted;
import com.kji.normalize.Extracted.ExtractionMethod;
import com.kji.normalize.ExperienceRequirement;
import com.kji.normalize.Lexicon;
import com.kji.normalize.TermMatcher;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class SeniorityClassifier {

    private final Lexicon lexicon;

    public SeniorityClassifier(Lexicon lexicon) {
        this.lexicon = lexicon;
    }

    public Extracted<Seniority> classify(Extracted<ExperienceRequirement> experience, String title) {
        Optional<String> excludedTerm =
                TermMatcher.matchedTerm(title, lexicon.seniorityExcluded());
        if (excludedTerm.isPresent()) {
            return Extracted.of(new Seniority("X", "Senior or above by title"), 0.90d,
                    excludedTerm.get(), ExtractionMethod.LEXICON);
        }

        if (!experience.isKnown()) {
            return Extracted.unknown(experience.evidence());
        }

        ExperienceRequirement requirement = experience.value();
        return switch (requirement.kind()) {
            case INTERN -> Extracted.of(new Seniority("A", "Internship"),
                    experience.confidence(), experience.evidence(), ExtractionMethod.LEXICON);
            case NEW_GRADUATE -> Extracted.of(new Seniority("A", "New graduate or entry level"),
                    experience.confidence(), experience.evidence(), ExtractionMethod.LEXICON);
            case NEW_GRADUATE_OR_EXPERIENCED -> Extracted.of(
                    new Seniority("B", "Open to new graduates and experienced candidates"),
                    experience.confidence(), experience.evidence(), ExtractionMethod.LEXICON);
            case ANY -> Extracted.of(new Seniority("B", "No stated experience requirement"),
                    experience.confidence() * 0.9d, experience.evidence(), ExtractionMethod.LEXICON);
            case EXPERIENCED -> fromYears(requirement, experience);
        };
    }

    private Extracted<Seniority> fromYears(ExperienceRequirement requirement,
                                           Extracted<ExperienceRequirement> experience) {
        Integer min = requirement.yearsMin();
        Integer max = requirement.yearsMax();

        if (min == null && max != null) {
            return Extracted.of(new Seniority("B", "Capped at " + max + " years of experience"),
                    experience.confidence(), experience.evidence(), ExtractionMethod.PATTERN_MATCH);
        }
        if (min == null) {
            return Extracted.unknown(experience.evidence());
        }
        if (min <= 2) {
            return Extracted.of(new Seniority("B", "Around " + min + " years, junior accessible"),
                    experience.confidence(), experience.evidence(), ExtractionMethod.PATTERN_MATCH);
        }
        if (min == 3) {
            return Extracted.of(new Seniority("C", "Three years stated, a stretch for a new graduate"),
                    experience.confidence(), experience.evidence(), ExtractionMethod.PATTERN_MATCH);
        }
        if (min <= 5) {
            return Extracted.of(new Seniority("D", min + " years stated, experienced only"),
                    experience.confidence(), experience.evidence(), ExtractionMethod.PATTERN_MATCH);
        }
        return Extracted.of(new Seniority("X", min + " years stated, senior or above"),
                experience.confidence(), experience.evidence(), ExtractionMethod.PATTERN_MATCH);
    }

    public record Seniority(String bucket, String label) {
    }
}
