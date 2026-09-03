package com.kji.ingest;

import com.kji.source.Source;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SearchRunService {

    private final SearchRunRepository repository;
    private final Clock clock;

    public SearchRunService(SearchRunRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SearchRun open(Source source, SearchRun.TriggerKind trigger, String queryText,
                          String queryParams, String collector) {
        return repository.save(new SearchRun(source.getId(), trigger, queryText, queryParams,
                Instant.now(clock), collector));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SearchRun complete(SearchRun run, SearchRun.Status status, String errorSummary) {
        run.complete(status, Instant.now(clock), errorSummary);
        return repository.save(run);
    }
}
