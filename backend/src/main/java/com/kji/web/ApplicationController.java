package com.kji.web;

import com.kji.crm.ApplicationService;
import com.kji.crm.ApplicationStatus;
import com.kji.web.dto.ApplicationResponse;
import com.kji.web.dto.PageResponse;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Locale;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationViewService viewService;

    public ApplicationController(ApplicationViewService viewService) {
        this.viewService = viewService;
    }

    @GetMapping
    public PageResponse<ApplicationResponse> list(
            @RequestParam(required = false) String profile,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return viewService.list(profile, parseStatus(status), page, size);
    }

    @GetMapping("/{id}")
    public ApplicationResponse detail(@PathVariable Long id) {
        return viewService.detail(id);
    }

    @PostMapping
    public ApplicationResponse create(@RequestBody UpsertRequest body) {
        return viewService.createOrUpdate(body.jobId(), body.profile(),
                parseStatus(body.status()), body.toDetails(), body.note());
    }

    @PatchMapping("/{id}")
    public ApplicationResponse update(@PathVariable Long id, @RequestBody UpsertRequest body) {
        return viewService.update(id, parseStatus(body.status()), body.toDetails(), body.note());
    }

    private ApplicationStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return ApplicationStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
    }

    public record UpsertRequest(
            @NotNull Long jobId,
            String profile,
            String status,
            String note,
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

        ApplicationService.Details toDetails() {
            return new ApplicationService.Details(resumeVersion, coverLetterVersion, contactName,
                    contactEmail, referral, interviewStage, interviewNotes, followUpAt, notes,
                    appliedAt);
        }
    }
}
