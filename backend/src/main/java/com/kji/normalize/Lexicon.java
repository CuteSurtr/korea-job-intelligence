package com.kji.normalize;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class Lexicon {

    private static final String RESOURCE_PATH = "normalize/lexicon.json";

    private final LexiconData data;
    private final List<CompiledExperiencePattern> experiencePatterns;

    public Lexicon(ObjectMapper objectMapper) {
        this.data = load(objectMapper);
        this.experiencePatterns = data.experiencePatterns().stream()
                .map(raw -> new CompiledExperiencePattern(
                        Pattern.compile(raw.regex(), Pattern.CASE_INSENSITIVE),
                        ExperiencePatternKind.valueOf(raw.kind())))
                .toList();
    }

    private LexiconData load(ObjectMapper objectMapper) {
        try (InputStream stream = new ClassPathResource(RESOURCE_PATH).getInputStream()) {
            return objectMapper.readValue(
                    new String(stream.readAllBytes(), StandardCharsets.UTF_8),
                    LexiconData.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load " + RESOURCE_PATH, exception);
        }
    }

    public List<String> companyLegalForms() {
        return data.companyLegalForms();
    }

    public List<String> titleBracketPrefixes() {
        return data.titleBracketPrefixes();
    }

    public List<String> openEndedDeadlineTerms() {
        return data.openEndedDeadlineTerms();
    }

    public List<String> deadlineSentinelDates() {
        return data.deadlineSentinelDates();
    }

    public List<String> deadlineTodayTerms() {
        return data.deadlineTodayTerms();
    }

    public List<String> deadlineTomorrowTerms() {
        return data.deadlineTomorrowTerms();
    }

    public List<String> experienceAny() {
        return data.experienceAny();
    }

    public List<String> experienceNewGraduate() {
        return data.experienceNewGraduate();
    }

    public List<String> experienceIntern() {
        return data.experienceIntern();
    }

    public List<String> experienceEitherNewGradOrExperienced() {
        return data.experienceEitherNewGradOrExperienced();
    }

    public List<CompiledExperiencePattern> experiencePatterns() {
        return experiencePatterns;
    }

    public int experienceImplausibleYears() {
        return data.experienceImplausibleYears();
    }

    public List<String> educationAny() {
        return data.educationAny();
    }

    public List<TermMapping> educationLevels() {
        return data.educationLevels();
    }

    public List<String> educationPreferredMarkers() {
        return data.educationPreferredMarkers();
    }

    public List<TermMapping> employmentTypes() {
        return data.employmentTypes();
    }

    public List<TermMapping> remotePolicies() {
        return data.remotePolicies();
    }

    public List<String> seniorityExcluded() {
        return data.seniorityExcluded();
    }

    public List<TermMapping> roleFamilies() {
        return data.roleFamilies();
    }

    public List<LocationAlias> locationCityAliases() {
        return data.locationCityAliases();
    }

    public enum ExperiencePatternKind {
        MINIMUM,
        MAXIMUM,
        RANGE
    }

    public record CompiledExperiencePattern(Pattern pattern, ExperiencePatternKind kind) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TermMapping(List<String> terms, String value, String level) {

        public String resolved() {
            return value != null ? value : level;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LocationAlias(List<String> terms, String city, String region) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RawExperiencePattern(String regex, String kind) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LexiconData(
            List<String> companyLegalForms,
            List<String> titleBracketPrefixes,
            List<String> openEndedDeadlineTerms,
            List<String> deadlineSentinelDates,
            List<String> deadlineTodayTerms,
            List<String> deadlineTomorrowTerms,
            List<String> experienceAny,
            List<String> experienceNewGraduate,
            List<String> experienceIntern,
            List<String> experienceEitherNewGradOrExperienced,
            List<RawExperiencePattern> experiencePatterns,
            int experienceImplausibleYears,
            List<String> educationAny,
            List<TermMapping> educationLevels,
            List<String> educationPreferredMarkers,
            List<TermMapping> employmentTypes,
            List<TermMapping> remotePolicies,
            List<String> seniorityExcluded,
            List<TermMapping> roleFamilies,
            List<LocationAlias> locationCityAliases
    ) {
    }
}
