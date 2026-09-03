package com.kji.scoring;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CareerValueScorerTest {

    private CareerValueScorer scorer;

    @BeforeEach
    void setUp() {
        scorer = new CareerValueScorer(new CareerValueWeights(new ObjectMapper()));
    }

    @Test
    @DisplayName("a backend role on a real production stack scores high and says why")
    void scoresProductionBackendHigh() {
        ScoreResult result = scorer.score(input("Staff Backend Engineer (Orchestration Platform)",
                longDescription("Design and operate the job scheduling platform in production."),
                "BACKEND",
                skills("java", "LANGUAGE", "postgresql", "DATABASE", "kubernetes", "INFRA",
                        "docker", "INFRA", "airflow", "INFRA", "aws", "CLOUD", "git", "TOOL")));

        assertThat(result.score()).isGreaterThan(55.0d);
        assertThat(result.explanation())
                .contains("Production software engineering role")
                .contains("Database work");
        assertThat(result.componentJson()).contains("production_engineering");
        assertThat(result.version()).isEqualTo("career-1");
    }

    @Test
    @DisplayName("a clerical role scores at the floor rather than at a middling default")
    void scoresClericalRoleAtFloor() {
        ScoreResult result = scorer.score(input("Recruiting Assistant",
                longDescription("Support the recruiting team with scheduling and general affairs."),
                null,
                List.of()));

        assertThat(result.score()).isZero();
        assertThat(result.explanation()).contains("Clerical");
    }

    @Test
    @DisplayName("a sales role is penalised even when the company is technical")
    void penalisesSalesRole() {
        ScoreResult result = scorer.score(input("Technical Account Manager (Global)",
                longDescription("Own the sales relationship for enterprise payment customers."),
                null,
                List.of()));

        assertThat(result.score()).isZero();
        assertThat(result.componentJson()).contains("sales");
    }

    @Test
    @DisplayName("manual labeling is penalised heavily even inside an engineering-sounding posting")
    void penalisesManualLabeling() {
        ScoreResult withLabeling = scorer.score(input("Data Operations Associate",
                longDescription("Perform data labeling for the training corpus every day."),
                null,
                skills("python", "LANGUAGE")));

        assertThat(withLabeling.componentJson()).contains("manual_labeling");
        assertThat(withLabeling.score()).isLessThan(20.0d);
    }

    @Test
    @DisplayName("a technical title with no development signal loses points for the gap")
    void penalisesTechnicalTitleWithoutDevelopmentSignal() {
        ScoreResult result = scorer.score(input("Backend Operations Engineer",
                longDescription("Monitor dashboards and escalate incidents to the platform team."),
                "BACKEND",
                skills("prometheus", "TOOL", "grafana", "TOOL")));

        assertThat(result.componentJson()).contains("no_development_signal");
    }

    @Test
    @DisplayName("automation cancels the manual-QA penalty rather than stacking with it")
    void automationCancelsManualQaPenalty() {
        ScoreResult manualOnly = scorer.score(input("QA Engineer",
                longDescription("Run manual testing passes before each release."),
                "QA", skills("python", "LANGUAGE")));
        ScoreResult automated = scorer.score(input("QA Engineer",
                longDescription("Run manual testing passes and own the automated suite."),
                "QA", skills("python", "LANGUAGE", "testing", "PRACTICE", "ci-cd", "PRACTICE")));

        assertThat(manualOnly.componentJson()).contains("manual_qa");
        assertThat(automated.componentJson()).doesNotContain("manual_qa");
        assertThat(automated.score()).isGreaterThan(manualOnly.score());
    }

    @Test
    @DisplayName("a thin posting scores with low confidence rather than with false precision")
    void thinPostingScoresWithLowConfidence() {
        ScoreResult thin = scorer.score(input("Backend Engineer", "Backend developer wanted.",
                "BACKEND", skills("java", "LANGUAGE")));

        assertThat(thin.confidence()).isLessThan(0.6d);
    }

    @Test
    @DisplayName("every score carries components and an explanation, never a bare number")
    void alwaysExplainsItself() {
        ScoreResult result = scorer.score(input("Backend Engineer",
                longDescription("Build and operate APIs."), "BACKEND",
                skills("java", "LANGUAGE", "spring-boot", "FRAMEWORK")));

        assertThat(result.componentJson()).startsWith("{\"components\":[");
        assertThat(result.explanation()).isNotBlank();
    }

    private ScoringInput input(String title, String description, String roleFamily,
                               List<ScoringInput.SkillSignal> skills) {
        return new ScoringInput(1L, title, description, roleFamily, "B", 0, null, null,
                "FULL_TIME", "ONSITE", "Seoul", "KR", null, false, "UNKNOWN", skills);
    }

    private List<ScoringInput.SkillSignal> skills(String... slugAndCategory) {
        List<ScoringInput.SkillSignal> signals = new java.util.ArrayList<>();
        for (int index = 0; index < slugAndCategory.length; index += 2) {
            signals.add(new ScoringInput.SkillSignal(
                    slugAndCategory[index], slugAndCategory[index + 1], "REQUIRED"));
        }
        return signals;
    }

    private String longDescription(String text) {
        return text + " ".repeat(1) + "x".repeat(400);
    }
}
