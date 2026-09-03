package com.kji.scoring;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class CareerValueWeights {

    private static final String RESOURCE_PATH = "scoring/career-value.json";

    private final Data data;

    public CareerValueWeights(ObjectMapper objectMapper) {
        try (InputStream stream = new ClassPathResource(RESOURCE_PATH).getInputStream()) {
            this.data = objectMapper.readValue(
                    new String(stream.readAllBytes(), StandardCharsets.UTF_8), Data.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load " + RESOURCE_PATH, exception);
        }
    }

    public String version() {
        return data.version();
    }

    public List<String> engineeringRoleFamilies() {
        return data.engineeringRoleFamilies();
    }

    public List<Component> components() {
        return data.components();
    }

    public List<Penalty> penalties() {
        return data.penalties();
    }

    public Penalty noDevelopmentSignalPenalty() {
        return data.noDevelopmentSignalPenalty();
    }

    public List<String> developmentSignalSkillCategories() {
        return data.developmentSignalSkillCategories();
    }

    public ThinExtraction thinExtraction() {
        return data.thinExtraction();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Component(String key, String label, int weight,
                            List<String> roleFamilies, List<String> skills) {

        public Component {
            roleFamilies = roleFamilies == null ? List.of() : List.copyOf(roleFamilies);
            skills = skills == null ? List.of() : List.copyOf(skills);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Penalty(String key, String label, int weight, List<String> terms,
                          List<String> cancelledBySkills) {

        public Penalty {
            terms = terms == null ? List.of() : List.copyOf(terms);
            cancelledBySkills = cancelledBySkills == null ? List.of() : List.copyOf(cancelledBySkills);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ThinExtraction(int minDescriptionLength, int minSkillCount, double confidencePenalty) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Data(
            String version,
            List<String> engineeringRoleFamilies,
            List<Component> components,
            List<Penalty> penalties,
            Penalty noDevelopmentSignalPenalty,
            List<String> developmentSignalSkillCategories,
            ThinExtraction thinExtraction
    ) {
    }
}
