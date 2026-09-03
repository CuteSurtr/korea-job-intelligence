package com.kji.web;

import com.kji.ingest.IngestionFailureRepository;
import com.kji.ingest.SearchRun;
import com.kji.ingest.SearchRunRepository;
import com.kji.source.Source;
import com.kji.source.SourceRepository;
import com.kji.web.dto.PageResponse;
import com.kji.web.dto.SearchRunResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search-runs")
public class SearchRunController {

    private final SearchRunRepository searchRunRepository;
    private final IngestionFailureRepository failureRepository;
    private final SourceRepository sourceRepository;

    public SearchRunController(SearchRunRepository searchRunRepository,
                               IngestionFailureRepository failureRepository,
                               SourceRepository sourceRepository) {
        this.searchRunRepository = searchRunRepository;
        this.failureRepository = failureRepository;
        this.sourceRepository = sourceRepository;
    }

    @GetMapping
    public PageResponse<SearchRunResponse> list(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "25") int size) {
        Map<Long, String> codes = sourceCodes();
        return PageResponse.from(
                searchRunRepository.findAllByOrderByStartedAtDesc(
                        PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)))),
                run -> SearchRunResponse.from(run, codes.getOrDefault(run.getSourceId(), "unknown"),
                        List.of()));
    }

    @GetMapping("/{id}")
    public SearchRunResponse detail(@PathVariable Long id) {
        SearchRun run = searchRunRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No search run with id " + id));
        List<SearchRunResponse.FailureSummary> failures =
                failureRepository.findBySearchRunIdOrderByOccurredAtAsc(id).stream()
                        .map(SearchRunResponse.FailureSummary::from)
                        .toList();
        return SearchRunResponse.from(run,
                sourceCodes().getOrDefault(run.getSourceId(), "unknown"), failures);
    }

    private Map<Long, String> sourceCodes() {
        Map<Long, String> codes = new HashMap<>();
        for (Source source : sourceRepository.findAll()) {
            codes.put(source.getId(), source.getCode());
        }
        return codes;
    }
}
