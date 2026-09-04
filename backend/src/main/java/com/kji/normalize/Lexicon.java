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
    private final List<CompiledSalaryPattern> salaryPatterns;

    public Lexicon(ObjectMapper objectMapper) {
        this.data = load(objectMapper);
        this.experiencePatterns = data.experiencePatterns().stream()
                .map(raw -> new CompiledExperiencePattern(
                        Pattern.compile(raw.regex(), Pattern.CASE_INSENSITIVE),
                        ExperiencePatternKind.valueOf(raw.kind())))
                .toList();
        this.salaryPatterns = data.salaryPatterns().stream()
                .map(raw -> new CompiledSalaryPattern(
                        Pattern.compile(raw.regex(), Pattern.CASE_INSENSITIVE),
                        raw.unit(),
                        SalaryPatternKind.valueOf(raw.kind())))
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

    /**
     * Industry terms, ordered so that the financial sectors are tested first.
     *
     * {@link TermMatcher#firstMatch} returns on the first mapping that hits, so order decides
     * ties. Finance leads because a posting that mentions both payments and commerce is far
     * more likely to be a payments company mentioning its market than the reverse.
     */
    public List<TermMapping> sectors() {
        return data.sectors();
    }

    public List<LocationAlias> locationCityAliases() {
        return data.locationCityAliases();
    }

    public List<SectionHeading> sectionHeadings() {
        return data.sectionHeadings();
    }

    public List<CompiledSalaryPattern> salaryPatterns() {
        return salaryPatterns;
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
    public record SectionHeading(String section, List<String> terms) {
    }

    public record CompiledSalaryPattern(Pattern pattern, String unit, SalaryPatternKind kind) {
    }

    public enum SalaryPatternKind {
        EXACT,
        RANGE
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RawSalaryPattern(String regex, String unit, String kind) {
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
            List<TermMapping> sectors,
            List<SectionHeading> sectionHeadings,
            List<RawSalaryPattern> salaryPatterns,
            List<LocationAlias> locationCityAliases
    ) {
    }
}
