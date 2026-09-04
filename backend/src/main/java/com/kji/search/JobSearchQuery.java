package com.kji.search;

import com.kji.job.LifecycleState;
import java.util.List;
import java.util.Locale;

public record JobSearchQuery(
        String keyword,
        String company,
        List<LifecycleState> lifecycleStates,
        String locationCity,
        String sourceCode,
        String roleFamily,
        List<String> sectors,
        String companyStage,
        Boolean excludeLarge,
        List<String> seniorityBuckets,
        Integer maxYearsExperience,
        Double minCareerValue,
        Double minCandidateFit,
        String remotePolicy,
        String degreeRequired,
        String companyRiskLevel,
        Integer postedWithinDays,
        Boolean openOnly,
        SortOrder sort,
        int page,
        int size
) {

    public JobSearchQuery {
        lifecycleStates = lifecycleStates == null ? List.of() : List.copyOf(lifecycleStates);
        sectors = sectors == null ? List.of() : List.copyOf(sectors);
        seniorityBuckets = seniorityBuckets == null ? List.of() : List.copyOf(seniorityBuckets);
        sort = sort == null ? SortOrder.BEST_MATCH : sort;
        page = Math.max(0, page);
        size = Math.max(1, Math.min(200, size));
    }

    public String cacheKey() {
        return String.join("|",
                normalize(keyword),
                normalize(company),
                joinSorted(lifecycleStates.stream().map(Enum::name).toList()),
                normalize(locationCity),
                normalize(sourceCode),
                normalize(roleFamily),
                joinSorted(sectors),
                normalize(companyStage),
                String.valueOf(excludeLarge),
                joinSorted(seniorityBuckets),
                String.valueOf(maxYearsExperience),
                String.valueOf(minCareerValue),
                String.valueOf(minCandidateFit),
                normalize(remotePolicy),
                normalize(degreeRequired),
                normalize(companyRiskLevel),
                String.valueOf(postedWithinDays),
                String.valueOf(openOnly),
                sort.name(),
                String.valueOf(page),
                String.valueOf(size));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String joinSorted(List<String> values) {
        return values.stream().sorted().reduce("", (left, right) -> left + "," + right);
    }

    public enum SortOrder {
        BEST_MATCH,
        HIGHEST_CAREER_VALUE,
        JUNIOR_FRIENDLY,
        NEWEST,
        CLOSING_SOON,
        RECENTLY_VERIFIED,
        MOST_SOURCES,
        COMPANY
    }
}
