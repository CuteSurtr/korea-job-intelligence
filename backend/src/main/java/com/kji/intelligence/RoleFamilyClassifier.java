package com.kji.intelligence;

import com.kji.normalize.Extracted;
import com.kji.normalize.Lexicon;
import com.kji.normalize.TermMatcher;
import org.springframework.stereotype.Component;

@Component
public class RoleFamilyClassifier {

    private final Lexicon lexicon;

    public RoleFamilyClassifier(Lexicon lexicon) {
        this.lexicon = lexicon;
    }

    public Extracted<String> classify(String title, String description) {
        Extracted<String> fromTitle = TermMatcher.firstMatch(lexicon.roleFamilies(), title, 0.90d);
        if (fromTitle.isKnown()) {
            return fromTitle;
        }
        return TermMatcher.firstMatch(lexicon.roleFamilies(), description, 0.65d);
    }
}
