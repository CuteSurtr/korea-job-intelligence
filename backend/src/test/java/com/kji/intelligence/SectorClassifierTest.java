package com.kji.intelligence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.kji.normalize.Extracted;
import com.kji.normalize.LexiconTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SectorClassifierTest {

    private final SectorClassifier classifier =
            new SectorClassifier(LexiconTestSupport.lexicon());

    @Test
    @DisplayName("a company's own name settles the sector, whatever the description talks about")
    void companyNameWins() {
        Extracted<String> result = classifier.classify(
                "카카오페이증권",
                "Backend Engineer",
                "우리는 커머스 팀과 협업하며 게임 서비스 연동도 담당합니다.");

        assertThat(result.value()).isEqualTo("SECURITIES");
        assertThat(result.confidence()).isEqualTo(0.85d, within(0.001d));
        assertThat(result.method()).isEqualTo(Extracted.ExtractionMethod.LEXICON);
    }

    @Test
    @DisplayName("a title carries the sector when the company name is anonymous")
    void titleIsUsedNext() {
        Extracted<String> result = classifier.classify(
                "주식회사 에이비씨",
                "핀테크 결제 플랫폼 백엔드 개발자",
                "");

        assertThat(result.value()).isEqualTo("FINTECH");
        assertThat(result.confidence()).isEqualTo(0.70d, within(0.001d));
    }

    @Test
    @DisplayName("the description is ignored, however clearly it names an industry")
    void descriptionIsNotEvidence() {
        // Measured over 236 real postings, thirteen of the seventeen sectors that came from
        // description text alone were wrong: robotics firms say "logistics", payment firms say
        // "은행", chip firms say "enterprise". None of it is a claim about what the job is.
        Extracted<String> result = classifier.classify(
                "주식회사 에이비씨",
                "Software Engineer",
                "자율주행 차량의 인지 모듈을 개발합니다.");

        assertThat(result.isKnown()).isFalse();
    }

    @Test
    @DisplayName("the same words in the title do settle it")
    void titleIsEvidenceWhereDescriptionIsNot() {
        Extracted<String> result = classifier.classify(
                "주식회사 에이비씨", "자율주행 인지 소프트웨어 엔지니어", "");

        assertThat(result.value()).isEqualTo("MOBILITY");
        assertThat(result.confidence()).isEqualTo(0.70d, within(0.001d));
    }

    @Test
    @DisplayName("nothing is invented when no sector term appears anywhere")
    void unknownStaysUnknown() {
        Extracted<String> result = classifier.classify(
                "주식회사 에이비씨", "Software Engineer", "좋은 동료와 함께 성장합니다.");

        assertThat(result.isKnown()).isFalse();
        assertThat(result.value()).isNull();
    }

    @Test
    @DisplayName("null and blank inputs are tolerated at every position")
    void nullsAreSafe() {
        assertThat(classifier.classify(null, null, null).isKnown()).isFalse();
        assertThat(classifier.classify("", "", "").isKnown()).isFalse();
        assertThat(classifier.classify(null, "블록체인 지갑 개발자", null).value()).isEqualTo("CRYPTO");
        // a description alone establishes nothing, even a vivid one
        assertThat(classifier.classify(null, null, "블록체인 지갑 서비스").isKnown()).isFalse();
    }

    @Test
    @DisplayName("finance is tested before the general sectors, so a fintech is not filed as commerce")
    void financeOutranksTheGeneralSectors() {
        // Payments companies describe the commerce they serve constantly. The company is still
        // a fintech, and ordering the lexicon finance-first is what encodes that.
        Extracted<String> result = classifier.classify(
                "주식회사 페이먼츠",
                "간편결제 서버 개발자",
                "이커머스 가맹점을 위한 결제 플랫폼을 만듭니다.");

        assertThat(result.value()).isEqualTo("FINTECH");
    }

    @Test
    @DisplayName("the financial sectors are exactly the five that make a role a finance role")
    void financialSetIsExplicit() {
        assertThat(SectorClassifier.financialSectors())
                .containsExactlyInAnyOrder("FINTECH", "BANKING", "SECURITIES", "CRYPTO", "INSURANCE");

        assertThat(SectorClassifier.isFinancial("FINTECH")).isTrue();
        assertThat(SectorClassifier.isFinancial("BANKING")).isTrue();
        assertThat(SectorClassifier.isFinancial("GAMING")).isFalse();
        assertThat(SectorClassifier.isFinancial("AI_ML")).isFalse();
        assertThat(SectorClassifier.isFinancial(null)).isFalse();
    }

    @Test
    @DisplayName("each financial sector is reachable from realistic Korean posting text")
    void everyFinancialSectorIsReachable() {
        assertThat(classifier.classify("토스페이먼츠", "서버 개발자", "").value()).isEqualTo("FINTECH");
        assertThat(classifier.classify("케이뱅크", "서버 개발자", "").value()).isEqualTo("BANKING");
        assertThat(classifier.classify("미래에셋증권", "서버 개발자", "").value()).isEqualTo("SECURITIES");
        assertThat(classifier.classify("두나무", "가상자산 거래소 백엔드 개발자", "").value())
                .isEqualTo("CRYPTO");
        assertThat(classifier.classify("캐롯손해보험", "서버 개발자", "").value()).isEqualTo("INSURANCE");
    }
}
