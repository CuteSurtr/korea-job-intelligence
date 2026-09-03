package com.kji.search;

import com.kji.job.LifecycleState;
import java.util.List;

public record JobSearchQuery(
        String keyword,
        String company,
        List<LifecycleState> lifecycleStates,
        String locationCity,
        String sourceCode,
        Boolean openOnly,
        SortOrder sort,
        int page,
        int size
) {

    public JobSearchQuery {
        lifecycleStates = lifecycleStates == null ? List.of() : List.copyOf(lifecycleStates);
        sort = sort == null ? SortOrder.NEWEST : sort;
        page = Math.max(0, page);
        size = Math.max(1, Math.min(200, size));
    }

    public String cacheKey() {
        return String.join("|",
                normalize(keyword),
                normalize(company),
                lifecycleStates.stream().map(Enum::name).sorted().reduce("", (a, b) -> a + "," + b),
                normalize(locationCity),
                normalize(sourceCode),
                String.valueOf(openOnly),
                sort.name(),
                String.valueOf(page),
                String.valueOf(size));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public enum SortOrder {
        NEWEST,
        RECENTLY_VERIFIED,
        CLOSING_SOON,
        MOST_SOURCES,
        COMPANY
    }
}
