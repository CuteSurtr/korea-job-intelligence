package com.kji.normalize;

import com.kji.normalize.Extracted.ExtractionMethod;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class EducationParser {

    private static final int PREFERRED_MARKER_WINDOW = 40;

    private final Lexicon lexicon;

    public EducationParser(Lexicon lexicon) {
        this.lexicon = lexicon;
    }

    public Result parse(String rawEducation, String requirementsText, String preferredText) {
        if (TermMatcher.containsAny(rawEducation, lexicon.educationAny())) {
            return new Result(Extracted.of("NONE", 0.90d, rawEducation, ExtractionMethod.LEXICON),
                    Extracted.unknown());
        }

        Extracted<String> fromPreferredSection = levelIn(preferredText, 0.80d);
        Extracted<String> fromRequirementsSection = levelIn(requirementsText, 0.85d);

        if (fromRequirementsSection.isKnown() || fromPreferredSection.isKnown()) {
            return new Result(fromRequirementsSection, fromPreferredSection);
        }

        Extracted<String> fromField = levelIn(rawEducation, 0.85d);
        if (!fromField.isKnown()) {
            return new Result(Extracted.unknown(rawEducation), Extracted.unknown());
        }
        if (nearPreferredMarker(rawEducation, fromField.evidence())) {
            return new Result(Extracted.unknown(rawEducation), fromField);
        }
        return new Result(fromField, Extracted.unknown());
    }

    private Extracted<String> levelIn(String text, double confidence) {
        return TermMatcher.firstMatch(lexicon.educationLevels(), text, confidence);
    }

    private boolean nearPreferredMarker(String text, String evidence) {
        if (TextNormalizer.isBlank(text) || evidence == null) {
            return false;
        }
        String lowered = text.toLowerCase(Locale.ROOT);
        int index = lowered.indexOf(evidence.toLowerCase(Locale.ROOT));
        if (index < 0) {
            return false;
        }
        int end = Math.min(lowered.length(), index + evidence.length() + PREFERRED_MARKER_WINDOW);
        String window = lowered.substring(index, end);
        return TermMatcher.containsAny(window, lexicon.educationPreferredMarkers());
    }

    public record Result(Extracted<String> required, Extracted<String> preferred) {

        public List<Extracted<String>> all() {
            return List.of(required, preferred);
        }
    }
}
