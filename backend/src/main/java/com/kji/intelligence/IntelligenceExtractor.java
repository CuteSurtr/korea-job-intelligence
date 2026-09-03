package com.kji.intelligence;

import com.kji.normalize.DescriptionSections;
import com.kji.normalize.EducationParser;
import com.kji.normalize.ExperienceParser;
import com.kji.normalize.ExperienceRequirement;
import com.kji.normalize.Extracted;
import com.kji.normalize.Lexicon;
import com.kji.normalize.SalaryParser;
import com.kji.normalize.TermMatcher;
import com.kji.normalize.TextNormalizer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IntelligenceExtractor {

    public static final String EXTRACTOR_VERSION = "intel-1";

    private final JobIntelligenceRepository intelligenceRepository;
    private final JobIntelligenceFieldRepository fieldRepository;
    private final JobSkillRepository jobSkillRepository;
    private final DescriptionSections descriptionSections;
    private final ExperienceParser experienceParser;
    private final EducationParser educationParser;
    private final SalaryParser salaryParser;
    private final SeniorityClassifier seniorityClassifier;
    private final RoleFamilyClassifier roleFamilyClassifier;
    private final SkillExtractor skillExtractor;
    private final Lexicon lexicon;
    private final Clock clock;

    public IntelligenceExtractor(JobIntelligenceRepository intelligenceRepository,
                                 JobIntelligenceFieldRepository fieldRepository,
                                 JobSkillRepository jobSkillRepository,
                                 DescriptionSections descriptionSections,
                                 ExperienceParser experienceParser,
                                 EducationParser educationParser,
                                 SalaryParser salaryParser,
                                 SeniorityClassifier seniorityClassifier,
                                 RoleFamilyClassifier roleFamilyClassifier,
                                 SkillExtractor skillExtractor,
                                 Lexicon lexicon,
                                 Clock clock) {
        this.intelligenceRepository = intelligenceRepository;
        this.fieldRepository = fieldRepository;
        this.jobSkillRepository = jobSkillRepository;
        this.descriptionSections = descriptionSections;
        this.experienceParser = experienceParser;
        this.educationParser = educationParser;
        this.salaryParser = salaryParser;
        this.seniorityClassifier = seniorityClassifier;
        this.roleFamilyClassifier = roleFamilyClassifier;
        this.skillExtractor = skillExtractor;
        this.lexicon = lexicon;
        this.clock = clock;
    }

    @Transactional
    public JobIntelligence extract(IntelligenceInput input) {
        Instant now = Instant.now(clock);
        DescriptionSections.Sections sections = descriptionSections.split(input.description());
        String requirementsText = sections.textOf(DescriptionSections.Section.REQUIREMENTS);
        String preferredText = sections.textOf(DescriptionSections.Section.PREFERRED);

        Extracted<ExperienceRequirement> experience = resolveExperience(input, requirementsText);
        Extracted<SeniorityClassifier.Seniority> seniority =
                seniorityClassifier.classify(experience, input.title());
        Extracted<String> roleFamily =
                roleFamilyClassifier.classify(input.title(), input.description());
        EducationParser.Result education =
                educationParser.parse(input.rawEducation(), requirementsText, preferredText);
        Extracted<String> employmentType = TermMatcher.firstMatch(lexicon.employmentTypes(),
                firstNonBlank(input.rawEmploymentType(), input.title()), 0.85d);
        Extracted<String> remotePolicy = TermMatcher.firstMatch(lexicon.remotePolicies(),
                firstNonBlank(input.rawRemotePolicy(), input.description()), 0.75d);
        Extracted<SalaryParser.Salary> salary = salaryParser.parse(input.description());

        JobIntelligence intelligence = intelligenceRepository.findByJobId(input.jobId())
                .orElseGet(() -> new JobIntelligence(input.jobId(), EXTRACTOR_VERSION, now));
        intelligence.apply(EXTRACTOR_VERSION, input.snapshotId(), now);
        intelligence.setRoleFamily(roleFamily.value());
        intelligence.setSeniority(
                seniority.isKnown() ? seniority.value().bucket() : null,
                seniority.isKnown() ? seniority.value().label() : null);
        intelligence.setYearsExperience(
                experience.isKnown() ? experience.value().yearsMin() : null,
                experience.isKnown() ? experience.value().yearsMax() : null);
        intelligence.setDegree(education.required().value(), education.preferred().value());
        intelligence.setEmploymentType(employmentType.value());
        intelligence.setRemotePolicy(normalizeRemotePolicy(remotePolicy.value()));
        intelligence.setLocation(input.locationRaw(), input.locationCity(),
                input.locationRegion(), input.locationCountry());
        intelligence.setSalary(
                salary.isKnown() ? salary.value().min() : null,
                salary.isKnown() ? salary.value().max() : null,
                salary.isKnown() ? salary.value().currency() : null,
                salary.isKnown() ? salary.value().period() : null);
        intelligence.setSections(
                sections.get(DescriptionSections.Section.RESPONSIBILITIES).toArray(String[]::new),
                sections.get(DescriptionSections.Section.REQUIREMENTS).toArray(String[]::new),
                sections.get(DescriptionSections.Section.PREFERRED).toArray(String[]::new));
        JobIntelligence saved = intelligenceRepository.save(intelligence);

        writeField(input, now, "role_family", roleFamily);
        writeField(input, now, "seniority_bucket", seniority.isKnown()
                ? Extracted.of(seniority.value().bucket(), seniority.confidence(),
                seniority.evidence(), seniority.method())
                : Extracted.unknown(seniority.evidence()));
        writeField(input, now, "years_experience_min", mapExperience(experience, true));
        writeField(input, now, "years_experience_max", mapExperience(experience, false));
        writeField(input, now, "degree_required", education.required());
        writeField(input, now, "degree_preferred", education.preferred());
        writeField(input, now, "employment_type", employmentType);
        writeField(input, now, "remote_policy", remotePolicy);
        writeField(input, now, "salary_min", mapSalary(salary, true));
        writeField(input, now, "salary_max", mapSalary(salary, false));

        writeSkills(input, sections);
        return saved;
    }

    private Extracted<ExperienceRequirement> resolveExperience(IntelligenceInput input,
                                                               String requirementsText) {
        Extracted<ExperienceRequirement> fromField = experienceParser.parse(input.rawExperience());
        if (fromField.isKnown()) {
            return fromField;
        }
        Extracted<ExperienceRequirement> fromRequirements = experienceParser.parse(requirementsText);
        if (fromRequirements.isKnown()) {
            return fromRequirements;
        }
        Extracted<ExperienceRequirement> fromTitle = experienceParser.parse(input.title());
        return fromTitle.isKnown() ? fromTitle : fromField;
    }

    private Extracted<String> mapExperience(Extracted<ExperienceRequirement> experience, boolean min) {
        if (!experience.isKnown()) {
            return Extracted.unknown(experience.evidence());
        }
        Integer value = min ? experience.value().yearsMin() : experience.value().yearsMax();
        if (value == null) {
            return Extracted.unknown(experience.evidence());
        }
        return Extracted.of(String.valueOf(value), experience.confidence(),
                experience.evidence(), experience.method());
    }

    private Extracted<String> mapSalary(Extracted<SalaryParser.Salary> salary, boolean min) {
        if (!salary.isKnown()) {
            return Extracted.unknown(salary.evidence());
        }
        Long value = min ? salary.value().min() : salary.value().max();
        if (value == null) {
            return Extracted.unknown(salary.evidence());
        }
        return Extracted.of(String.valueOf(value), salary.confidence(),
                salary.evidence(), salary.method());
    }

    private void writeField(IntelligenceInput input, Instant now, String fieldName,
                            Extracted<String> extracted) {
        if (!extracted.isKnown()) {
            fieldRepository.findByJobIdAndFieldNameAndExtractorVersion(
                            input.jobId(), fieldName, EXTRACTOR_VERSION)
                    .ifPresent(fieldRepository::delete);
            return;
        }
        JobIntelligenceField.ExtractionMethod method =
                JobIntelligenceField.ExtractionMethod.valueOf(extracted.method().name());
        BigDecimal confidence = BigDecimal.valueOf(extracted.confidence())
                .setScale(3, RoundingMode.HALF_UP);

        fieldRepository.findByJobIdAndFieldNameAndExtractorVersion(
                        input.jobId(), fieldName, EXTRACTOR_VERSION)
                .ifPresentOrElse(
                        existing -> {
                            existing.update(extracted.value(), confidence, extracted.evidence(),
                                    input.snapshotId(), input.sourceId(), method, now);
                            fieldRepository.save(existing);
                        },
                        () -> fieldRepository.save(new JobIntelligenceField(
                                input.jobId(), fieldName, extracted.value(), confidence,
                                extracted.evidence(), input.snapshotId(), input.sourceId(),
                                method, EXTRACTOR_VERSION, now)));
    }

    private void writeSkills(IntelligenceInput input, DescriptionSections.Sections sections) {
        List<SkillExtractor.Detection> detections = skillExtractor.extract(sections, input.title());
        jobSkillRepository.deleteByJobId(input.jobId());
        for (SkillExtractor.Detection detection : detections) {
            jobSkillRepository.save(new JobSkill(
                    input.jobId(),
                    detection.skillSlug(),
                    detection.level(),
                    BigDecimal.valueOf(detection.confidence()).setScale(3, RoundingMode.HALF_UP),
                    TextNormalizer.truncate(detection.evidence(), 400),
                    input.snapshotId(),
                    EXTRACTOR_VERSION));
        }
    }

    private String normalizeRemotePolicy(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "REMOTE", "HYBRID", "ONSITE" -> value;
            default -> "UNKNOWN";
        };
    }

    private String firstNonBlank(String first, String second) {
        return TextNormalizer.isBlank(first) ? second : first;
    }
}
