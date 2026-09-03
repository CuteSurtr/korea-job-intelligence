package com.kji.web.dto;

import com.kji.crm.Application;
import com.kji.crm.ApplicationStatusHistory;
import java.time.Instant;
import java.util.List;

public record ApplicationResponse(
        Long id,
        Long jobId,
        String jobTitle,
        String companyName,
        Long profileId,
        String profileCode,
        String status,
        Instant appliedAt,
        String resumeVersion,
        String coverLetterVersion,
        String contactName,
        String contactEmail,
        String referral,
        String interviewStage,
        String interviewNotes,
        Instant followUpAt,
        Instant rejectionAt,
        Instant offerAt,
        String notes,
        Instant createdAt,
        Instant updatedAt,
        List<StatusChange> history
) {

    public static ApplicationResponse from(Application application, String jobTitle,
                                           String companyName, String profileCode,
                                           List<StatusChange> history) {
        return new ApplicationResponse(
                application.getId(),
                application.getJobId(),
                jobTitle,
                companyName,
                application.getProfileId(),
                profileCode,
                application.getStatus().name(),
                application.getAppliedAt(),
                application.getResumeVersion(),
                application.getCoverLetterVersion(),
                application.getContactName(),
                application.getContactEmail(),
                application.getReferral(),
                application.getInterviewStage(),
                application.getInterviewNotes(),
                application.getFollowUpAt(),
                application.getRejectionAt(),
                application.getOfferAt(),
                application.getNotes(),
                application.getCreatedAt(),
                application.getUpdatedAt(),
                history);
    }

    public record StatusChange(String fromStatus, String toStatus, Instant changedAt, String note) {

        public static StatusChange from(ApplicationStatusHistory history) {
            return new StatusChange(
                    history.getFromStatus() == null ? null : history.getFromStatus().name(),
                    history.getToStatus().name(),
                    history.getChangedAt(),
                    history.getNote());
        }
    }
}
