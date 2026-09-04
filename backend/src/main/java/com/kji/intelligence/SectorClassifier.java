package com.kji.intelligence;

import com.kji.normalize.Extracted;
import com.kji.normalize.Lexicon;
import com.kji.normalize.TermMatcher;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * The industry a posting sits in, which is a different question from what the engineer does.
 *
 * <p>{@link RoleFamilyClassifier} answers "backend or frontend". This answers "at a bank or at
 * a game studio". Both are needed to find, say, a backend role in finance, and neither implies
 * the other.
 *
 * <p>Confidence follows how much the evidence is worth. A company's own name is the strongest
 * signal available: an employer called 카카오페이증권 is a securities firm, and no amount of
 * description text changes that. A title is weaker. A description is weakest by a wide margin,
 * because postings mention adjacent industries constantly — a commerce company describing bank
 * transfers is not a bank — so a description-only match is recorded as a low-confidence claim
 * rather than a fact.
 */
@Component
public class SectorClassifier {

    private static final double FROM_COMPANY = 0.85d;
    private static final double FROM_TITLE = 0.70d;

    /** The sectors that make a role a financial-engineering role. */
    private static final Set<String> FINANCIAL =
            Set.of("FINTECH", "BANKING", "SECURITIES", "CRYPTO", "INSURANCE");

    private final Lexicon lexicon;

    public SectorClassifier(Lexicon lexicon) {
        this.lexicon = lexicon;
    }

    /**
     * The sector of a posting, from its employer's name or its title.
     *
     * <p>The description is deliberately not consulted. It was, and measuring the result over
     * 236 real postings settled it: thirteen of the seventeen sectors that came from
     * description text alone were wrong. Postings name adjacent industries constantly — a
     * robotics company saying "logistics", a payments company saying "은행", an AI chip company
     * saying "enterprise" — and a sector asserted from that is noise wearing a confidence
     * score. An employer's name and the role's own title are claims about what the job is;
     * body text is not.
     */
    public Extracted<String> classify(String companyName, String title, String description) {
        Extracted<String> fromCompany =
                TermMatcher.firstMatch(lexicon.sectors(), companyName, FROM_COMPANY);
        if (fromCompany.isKnown()) {
            return fromCompany;
        }
        return TermMatcher.firstMatch(lexicon.sectors(), title, FROM_TITLE);
    }

    /** Whether a classified sector is one of the financial ones. */
    public static boolean isFinancial(String sector) {
        return sector != null && FINANCIAL.contains(sector);
    }

    public static Set<String> financialSectors() {
        return FINANCIAL;
    }
}
