package com.kji.crm;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "applications")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ApplicationStatus status = ApplicationStatus.NOT_REVIEWED;

    @Column(name = "applied_at")
    private Instant appliedAt;

    @Column(name = "resume_version", length = 120)
    private String resumeVersion;

    @Column(name = "cover_letter_version", length = 120)
    private String coverLetterVersion;

    @Column(name = "contact_name", length = 160)
    private String contactName;

    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @Column(length = 255)
    private String referral;

    @Column(name = "interview_stage", length = 120)
    private String interviewStage;

    @Column(name = "interview_notes")
    private String interviewNotes;

    @Column(name = "follow_up_at")
    private Instant followUpAt;

    @Column(name = "rejection_at")
    private Instant rejectionAt;

    @Column(name = "offer_at")
    private Instant offerAt;

    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Application() {
    }

    public Application(Long jobId, Long profileId, ApplicationStatus status) {
        this.jobId = jobId;
        this.profileId = profileId;
        this.status = status == null ? ApplicationStatus.NOT_REVIEWED : status;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public void applyStatus(ApplicationStatus next, Instant at) {
        this.status = next;
        switch (next) {
            case APPLIED -> {
                if (appliedAt == null) {
                    appliedAt = at;
                }
            }
            case REJECTED -> rejectionAt = at;
            case OFFER -> offerAt = at;
            default -> {
            }
        }
    }

    public void updateDetails(String resumeVersion, String coverLetterVersion, String contactName,
                              String contactEmail, String referral, String interviewStage,
                              String interviewNotes, Instant followUpAt, String notes,
                              Instant appliedAt) {
        if (resumeVersion != null) {
            this.resumeVersion = resumeVersion;
        }
        if (coverLetterVersion != null) {
            this.coverLetterVersion = coverLetterVersion;
        }
        if (contactName != null) {
            this.contactName = contactName;
        }
        if (contactEmail != null) {
            this.contactEmail = contactEmail;
        }
        if (referral != null) {
            this.referral = referral;
        }
        if (interviewStage != null) {
            this.interviewStage = interviewStage;
        }
        if (interviewNotes != null) {
            this.interviewNotes = interviewNotes;
        }
        if (followUpAt != null) {
            this.followUpAt = followUpAt;
        }
        if (notes != null) {
            this.notes = notes;
        }
        if (appliedAt != null) {
            this.appliedAt = appliedAt;
        }
    }

    public Long getId() {
        return id;
    }

    public Long getJobId() {
        return jobId;
    }

    public Long getProfileId() {
        return profileId;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public Instant getAppliedAt() {
        return appliedAt;
    }

    public String getResumeVersion() {
        return resumeVersion;
    }

    public String getCoverLetterVersion() {
        return coverLetterVersion;
    }

    public String getContactName() {
        return contactName;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public String getReferral() {
        return referral;
    }

    public String getInterviewStage() {
        return interviewStage;
    }

    public String getInterviewNotes() {
        return interviewNotes;
    }

    public Instant getFollowUpAt() {
        return followUpAt;
    }

    public Instant getRejectionAt() {
        return rejectionAt;
    }

    public Instant getOfferAt() {
        return offerAt;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
