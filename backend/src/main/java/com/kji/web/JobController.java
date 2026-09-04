package com.kji.web;

import com.kji.intelligence.SectorClassifier;
import com.kji.job.LifecycleState;
import com.kji.search.JobSearchQuery;
import com.kji.web.dto.JobDetailResponse;
import com.kji.web.dto.JobResponse;
import com.kji.web.dto.PageResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class JobController {

    private final JobQueryService queryService;
    private final JobDetailAssembler detailAssembler;

    public JobController(JobQueryService queryService, JobDetailAssembler detailAssembler) {
        this.queryService = queryService;
        this.detailAssembler = detailAssembler;
    }

    @GetMapping({"/jobs", "/search"})
    public PageResponse<JobResponse> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String roleFamily,
            @RequestParam(required = false) String sector,
            @RequestParam(required = false) Boolean financialOnly,
            @RequestParam(required = false) String seniority,
            @RequestParam(required = false) Integer maxYearsExperience,
            @RequestParam(required = false) Double minCareerValue,
            @RequestParam(required = false) Double minCandidateFit,
            @RequestParam(required = false) String remotePolicy,
            @RequestParam(required = false) String degreeRequired,
            @RequestParam(required = false) String companyRisk,
            @RequestParam(required = false) Integer postedWithinDays,
            @RequestParam(defaultValue = "true") boolean openOnly,
            @RequestParam(defaultValue = "BEST_MATCH") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {

        JobSearchQuery query = new JobSearchQuery(keyword, company, parseStates(state), location,
                source, roleFamily, resolveSectors(sector, financialOnly), splitUpper(seniority),
                maxYearsExperience, minCareerValue,
                minCandidateFit, remotePolicy, degreeRequired, companyRisk, postedWithinDays,
                openOnly, parseSort(sort), page, size);
        return queryService.list(query);
    }

    @GetMapping("/jobs/{id}")
    public JobDetailResponse detail(@PathVariable Long id) {
        return detailAssembler.assemble(id);
    }

    /**
     * Turns the two ways of asking for a sector into the one list searching understands.
     *
     * <p>{@code financialOnly} exists because "financial engineering roles" is the question
     * people actually have, and which of the five financial sectors a posting landed in is not
     * something they should need to know to ask it. Naming a sector explicitly still works, and
     * the two combine: sector=BANKING with financialOnly=true is just BANKING.
     */
    private List<String> resolveSectors(String sector, Boolean financialOnly) {
        List<String> named = splitUpper(sector);
        if (!Boolean.TRUE.equals(financialOnly)) {
            return named;
        }
        if (named.isEmpty()) {
            return List.copyOf(SectorClassifier.financialSectors());
        }
        return named.stream().filter(SectorClassifier::isFinancial).toList();
    }

    private List<LifecycleState> parseStates(String state) {
        return splitUpper(state).stream().map(LifecycleState::valueOf).toList();
    }

    private List<String> splitUpper(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(candidate -> !candidate.isEmpty())
                .map(candidate -> candidate.toUpperCase(Locale.ROOT))
                .toList();
    }

    private JobSearchQuery.SortOrder parseSort(String sort) {
        try {
            return JobSearchQuery.SortOrder.valueOf(sort.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown sort order: " + sort);
        }
    }
}
