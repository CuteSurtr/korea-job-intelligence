package com.kji.crm;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationStatusHistoryRepository historyRepository;
    private final Clock clock;

    public ApplicationService(ApplicationRepository applicationRepository,
                              ApplicationStatusHistoryRepository historyRepository,
                              Clock clock) {
        this.applicationRepository = applicationRepository;
        this.historyRepository = historyRepository;
        this.clock = clock;
    }

    @Transactional
    public Application createOrUpdate(Long jobId, Long profileId, ApplicationStatus status,
                                      Details details, String note) {
        Instant now = Instant.now(clock);
        return applicationRepository.findByJobIdAndProfileId(jobId, profileId)
                .map(existing -> update(existing, status, details, note, now))
                .orElseGet(() -> create(jobId, profileId, status, details, note, now));
    }

    @Transactional
    public Application update(Long applicationId, ApplicationStatus status, Details details,
                              String note) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(
                        "No application with id " + applicationId));
        return update(application, status, details, note, Instant.now(clock));
    }

    @Transactional(readOnly = true)
    public Page<Application> list(Long profileId, ApplicationStatus status, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(200, Math.max(1, size)));
        return status == null
                ? applicationRepository.findByProfileIdOrderByUpdatedAtDesc(profileId, pageable)
                : applicationRepository.findByProfileIdAndStatusOrderByUpdatedAtDesc(
                        profileId, status, pageable);
    }

    /** The application a profile has for one job, of which there is at most one. */
    @Transactional(readOnly = true)
    public Optional<Application> find(Long jobId, Long profileId) {
        return applicationRepository.findByJobIdAndProfileId(jobId, profileId);
    }

    @Transactional(readOnly = true)
    public Application require(Long applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(
                        "No application with id " + applicationId));
    }

    @Transactional(readOnly = true)
    public List<ApplicationStatusHistory> history(Long applicationId) {
        return historyRepository.findByApplicationIdOrderByChangedAtAsc(applicationId);
    }

    @Transactional(readOnly = true)
    public List<Application> forJob(Long jobId) {
        return applicationRepository.findByJobId(jobId);
    }

    private Application create(Long jobId, Long profileId, ApplicationStatus status,
                               Details details, String note, Instant now) {
        ApplicationStatus initial = status == null ? ApplicationStatus.NOT_REVIEWED : status;
        Application application = new Application(jobId, profileId, initial);
        application.applyStatus(initial, now);
        applyDetails(application, details);
        Application saved = applicationRepository.save(application);
        historyRepository.save(new ApplicationStatusHistory(saved.getId(), null, initial, now, note));
        return saved;
    }

    private Application update(Application application, ApplicationStatus status,
                               Details details, String note, Instant now) {
        ApplicationStatus previous = application.getStatus();
        if (status != null && status != previous) {
            application.applyStatus(status, now);
            historyRepository.save(new ApplicationStatusHistory(
                    application.getId(), previous, status, now, note));
        }
        applyDetails(application, details);
        return applicationRepository.save(application);
    }

    private void applyDetails(Application application, Details details) {
        if (details == null) {
            return;
        }
        application.updateDetails(details.resumeVersion(), details.coverLetterVersion(),
                details.contactName(), details.contactEmail(), details.referral(),
                details.interviewStage(), details.interviewNotes(), details.followUpAt(),
                details.notes(), details.appliedAt());
    }

    public record Details(
            String resumeVersion,
            String coverLetterVersion,
            String contactName,
            String contactEmail,
            String referral,
            String interviewStage,
            String interviewNotes,
            Instant followUpAt,
            String notes,
            Instant appliedAt
    ) {
    }

    public static class ApplicationNotFoundException extends RuntimeException {

        public ApplicationNotFoundException(String message) {
            super(message);
        }
    }
}
