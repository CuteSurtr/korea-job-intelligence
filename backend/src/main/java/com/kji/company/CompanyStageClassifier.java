package com.kji.company;

import com.kji.normalize.Lexicon;
import com.kji.normalize.TermMatcher;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Whether an employer is one of the large, well-known ones.
 *
 * <p>The product question is "show me roles I would not already have found", and the honest
 * form of that is not a funding stage. It is whether a Korean software engineer would recognise
 * the company without being told. So there are two answers and a silence: LARGE for names that
 * match the curated list, EMERGING for employers whose postings we found on their own applicant
 * tracking board, and null for everyone else.
 *
 * <p>The board signal is the interesting half. Aggregators carry whoever pays to advertise,
 * which skews to the companies everyone can already see. An employer running its own Greenhouse,
 * Ashby or Lever board and not appearing in the large list is close to a definition of the
 * population this project exists to surface. It is a weaker claim than a name match, so it is
 * recorded with what produced it and can be revised when better evidence arrives.
 */
@Component
public class CompanyStageClassifier {

    public static final String LARGE = "LARGE";
    public static final String EMERGING = "EMERGING";

    private final Lexicon lexicon;

    public CompanyStageClassifier(Lexicon lexicon) {
        this.lexicon = lexicon;
    }

    /**
     * @param companyName          the employer's canonical name
     * @param foundOnEmployerBoard whether this posting came from the employer's own ATS board
     *                             rather than an aggregator. The caller decides what counts,
     *                             because this module may not depend on the source registry.
     */
    public Stage classify(String companyName, boolean foundOnEmployerBoard) {
        Optional<String> matched = TermMatcher.matchedTerm(companyName, lexicon.largeEmployers());
        if (matched.isPresent()) {
            return new Stage(LARGE, "name matches " + matched.get());
        }
        if (foundOnEmployerBoard) {
            return new Stage(EMERGING, "posts on its own applicant tracking board");
        }
        return new Stage(null, null);
    }

    /** A stage and the evidence for it, or a pair of nulls when nothing established one. */
    public record Stage(String value, String evidence) {

        public boolean isKnown() {
            return value != null;
        }
    }
}
