package com.kji.web;

import com.kji.search.JobSearchQuery;
import com.kji.search.JobSearchService;
import com.kji.web.dto.JobResponse;
import com.kji.web.dto.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobQueryService {

    private final JobSearchService searchService;

    public JobQueryService(JobSearchService searchService) {
        this.searchService = searchService;
    }

    @Transactional(readOnly = true)
    public PageResponse<JobResponse> list(JobSearchQuery query) {
        return PageResponse.from(searchService.search(query), JobResponse::from);
    }
}
