package com.kji.scoring;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScoringService {

    private static final Logger log = LoggerFactory.getLogger(ScoringService.class);
    public static final String DEFAULT_PROFILE_CODE = "default";

    private final CareerValueScorer careerValueScorer;
    private final CandidateFitScorer candidateFitScorer;
    private final ApplicationPriorityScorer priorityScorer;
    private final JobScoreRepository scoreRepository;
    private final CandidateProfileRepository profileRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public ScoringService(CareerValueScorer careerValueScorer,
                          CandidateFitScorer candidateFitScorer,
                          ApplicationPriorityScorer priorityScorer,
                          JobScoreRepository scoreRepository,
                          CandidateProfileRepository profileRepository,
                          ObjectMapper objectMapper,
                          Clock clock) {
        this.careerValueScorer = careerValueScorer;
        this.candidateFitScorer = candidateFitScorer;
        this.priorityScorer = priorityScorer;
        this.scoreRepository = scoreRepository;
        this.profileRepository = profileRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public Scores score(ScoringInput input) {
        Instant now = Instant.now(clock);
        ScoreResult careerValue = careerValueScorer.score(input);
        persist(input.jobId(), JobScore.Kind.CAREER_VALUE, null, careerValue, now);

        List<CandidateProfile> profiles = profileRepository.findByActiveTrue();
        ScoreResult primaryFit = null;
        ScoreResult primaryPriority = null;

        for (CandidateProfile profile : profiles) {
            CandidateProfileData data = parse(profile);
            if (data == null) {
                continue;
            }
            ScoreResult fit = candidateFitScorer.score(input, data);
            ScoreResult priority = priorityScorer.score(input, careerValue, fit);
            persist(input.jobId(), JobScore.Kind.CANDIDATE_FIT, profile.getId(), fit, now);
            persist(input.jobId(), JobScore.Kind.APPLICATION_PRIORITY, profile.getId(), priority, now);

            if (DEFAULT_PROFILE_CODE.equals(profile.getCode()) || primaryFit == null) {
                primaryFit = fit;
                primaryPriority = priority;
            }
        }
        return new Scores(careerValue, primaryFit, primaryPriority);
    }

    private void persist(Long jobId, JobScore.Kind kind, Long profileId,
                         ScoreResult result, Instant now) {
        BigDecimal score = BigDecimal.valueOf(result.score()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal confidence = BigDecimal.valueOf(result.confidence())
                .setScale(3, RoundingMode.HALF_UP);

        scoreRepository.findExisting(jobId, kind, result.version(), profileId)
                .ifPresentOrElse(
                        existing -> {
                            existing.update(score, confidence, result.componentJson(),
                                    result.explanation(), now);
                            scoreRepository.save(existing);
                        },
                        () -> scoreRepository.save(new JobScore(jobId, kind, profileId, score,
                                result.version(), confidence, result.componentJson(),
                                result.explanation(), now)));
    }

    private CandidateProfileData parse(CandidateProfile profile) {
        try {
            return objectMapper.readValue(profile.getProfile(), CandidateProfileData.class);
        } catch (JsonProcessingException exception) {
            log.warn("candidate profile {} could not be parsed and was skipped: {}",
                    profile.getCode(), exception.getMessage());
            return null;
        }
    }

    public record Scores(ScoreResult careerValue, ScoreResult candidateFit, ScoreResult priority) {
    }
}
