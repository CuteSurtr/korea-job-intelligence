package com.kji.web;

import com.kji.crm.Application;
import com.kji.crm.ApplicationService;
import com.kji.crm.ApplicationStatus;
import com.kji.job.Job;
import com.kji.job.JobRepository;
import com.kji.scoring.CandidateProfile;
import com.kji.scoring.CandidateProfileRepository;
import com.kji.scoring.ScoringService;
import com.kji.web.dto.ApplicationResponse;
import com.kji.web.dto.PageResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationViewService {

    private final ApplicationService applicationService;
    private final JobRepository jobRepository;
    private final CandidateProfileRepository profileRepository;

    public ApplicationViewService(ApplicationService applicationService,
                                  JobRepository jobRepository,
                                  CandidateProfileRepository profileRepository) {
        this.applicationService = applicationService;
        this.jobRepository = jobRepository;
        this.profileRepository = profileRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ApplicationResponse> list(String profileCode, ApplicationStatus status,
                                                  int page, int size) {
        CandidateProfile profile = requireProfile(profileCode);
        return PageResponse.from(
                applicationService.list(profile.getId(), status, page, size),
                application -> toResponse(application, profile.getCode(), false));
    }

    @Transactional(readOnly = true)
    public ApplicationResponse detail(Long applicationId) {
        Application application = applicationService.require(applicationId);
        return toResponse(application, profileCode(application.getProfileId()), true);
    }

    @Transactional
    public ApplicationResponse createOrUpdate(Long jobId, String profileCode,
                                              ApplicationStatus status,
                                              ApplicationService.Details details, String note) {
        CandidateProfile profile = requireProfile(profileCode);
        Application application = applicationService.createOrUpdate(
                jobId, profile.getId(), status, details, note);
        return toResponse(application, profile.getCode(), true);
    }

    @Transactional
    public ApplicationResponse update(Long applicationId, ApplicationStatus status,
                                      ApplicationService.Details details, String note) {
        Application application = applicationService.update(applicationId, status, details, note);
        return toResponse(application, profileCode(application.getProfileId()), true);
    }

    public CandidateProfile requireProfile(String code) {
        String resolved = code == null || code.isBlank()
                ? ScoringService.DEFAULT_PROFILE_CODE
                : code;
        return profileRepository.findByCode(resolved)
                .orElseThrow(() -> new ResourceNotFoundException("No candidate profile " + resolved));
    }

    private ApplicationResponse toResponse(Application application, String profileCode,
                                           boolean includeHistory) {
        Job job = jobRepository.findById(application.getJobId()).orElse(null);
        List<ApplicationResponse.StatusChange> history = includeHistory
                ? applicationService.history(application.getId()).stream()
                        .map(ApplicationResponse.StatusChange::from)
                        .toList()
                : List.of();
        return ApplicationResponse.from(application,
                job == null ? null : job.getCanonicalTitle(),
                job == null ? null : job.getCompany().getCanonicalName(),
                profileCode, history);
    }

    private String profileCode(Long profileId) {
        return profileRepository.findById(profileId)
                .map(CandidateProfile::getCode)
                .orElse(null);
    }
}
