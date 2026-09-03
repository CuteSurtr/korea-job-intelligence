package com.kji.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.kji.cache.SearchResultCache;
import com.kji.search.JobSearchQuery;
import com.kji.search.JobSearchService;
import com.kji.web.dto.JobResponse;
import com.kji.web.dto.PageResponse;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobQueryService {

    private static final TypeReference<PageResponse<JobResponse>> PAGE_TYPE =
            new TypeReference<>() {
            };

    private final JobSearchService searchService;
    private final SearchResultCache cache;

    public JobQueryService(JobSearchService searchService, SearchResultCache cache) {
        this.searchService = searchService;
        this.cache = cache;
    }

    @Transactional(readOnly = true)
    public PageResponse<JobResponse> list(JobSearchQuery query) {
        String cacheKey = "jobs:" + query.cacheKey();
        Optional<PageResponse<JobResponse>> cached = cache.read(cacheKey, PAGE_TYPE);
        if (cached.isPresent()) {
            return cached.get();
        }
        PageResponse<JobResponse> page =
                PageResponse.from(searchService.search(query), JobResponse::from);
        cache.write(cacheKey, page);
        return page;
    }
}
