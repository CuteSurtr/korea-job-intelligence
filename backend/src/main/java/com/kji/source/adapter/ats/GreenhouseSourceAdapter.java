package com.kji.source.adapter.ats;

import com.kji.source.AdapterKind;
import com.kji.source.RawJobRecord;
import com.kji.source.SourceAdapter;
import com.kji.source.SourceException;
import com.kji.source.SourceFetchResult;
import com.kji.source.SourceQuery;
import com.kji.source.http.HttpJsonClient;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GreenhouseSourceAdapter implements SourceAdapter {

    private static final String DEFAULT_BASE_URL = "https://boards-api.greenhouse.io";

    private final HttpJsonClient httpClient;
    private final GreenhouseBoardParser parser;
    private final Clock clock;
    private final String baseUrl;

    @Autowired
    public GreenhouseSourceAdapter(HttpJsonClient httpClient,
                                   GreenhouseBoardParser parser,
                                   Clock clock) {
        this(httpClient, parser, clock, DEFAULT_BASE_URL);
    }

    GreenhouseSourceAdapter(HttpJsonClient httpClient,
                            GreenhouseBoardParser parser,
                            Clock clock,
                            String baseUrl) {
        this.httpClient = httpClient;
        this.parser = parser;
        this.clock = clock;
        this.baseUrl = baseUrl;
    }

    @Override
    public String sourceCode() {
        return GreenhouseBoardParser.SOURCE_CODE;
    }

    @Override
    public AdapterKind adapterKind() {
        return AdapterKind.ATS;
    }

    @Override
    public SourceFetchResult fetch(SourceQuery query) {
        String board = query.requiredParameter("board");
        URI uri = URI.create(baseUrl + "/v1/boards/" + board + "/jobs?content=true");
        HttpJsonClient.JsonResponse response = httpClient.getJson(uri);
        Instant fetchedAt = Instant.now(clock);

        List<RawJobRecord> matching = parser.parse(response.body(), board, fetchedAt).stream()
                .filter(record -> matchesLocation(record, query.parameter("location")))
                .filter(record -> matchesKeyword(record, query.queryText()))
                .toList();
        List<RawJobRecord> bounded = matching.size() <= query.maxRecords()
                ? matching
                : matching.subList(0, query.maxRecords());

        return bounded.size() == matching.size()
                ? SourceFetchResult.complete(bounded, response.latencyMillis())
                : SourceFetchResult.partial(bounded, 0, response.latencyMillis());
    }

    @Override
    public boolean supportsDirectVerification() {
        return true;
    }

    @Override
    public VerificationOutcome verify(String externalKey, SourceQuery query) {
        String board = query.parameter("board");
        if (board == null || externalKey == null) {
            return VerificationOutcome.INCONCLUSIVE;
        }
        String jobId = externalKey.startsWith(board + ":")
                ? externalKey.substring(board.length() + 1)
                : externalKey;
        URI uri = URI.create(baseUrl + "/v1/boards/" + board + "/jobs/" + jobId);
        try {
            httpClient.getJson(uri);
            return VerificationOutcome.PRESENT;
        } catch (SourceException exception) {
            Integer status = exception.httpStatus();
            if (status != null && (status == 404 || status == 410)) {
                return VerificationOutcome.ABSENT;
            }
            return VerificationOutcome.ERROR;
        }
    }

    private boolean matchesLocation(RawJobRecord record, String location) {
        if (location == null || location.isBlank()) {
            return true;
        }
        String candidate = record.rawLocation();
        return candidate != null
                && candidate.toLowerCase(Locale.ROOT).contains(location.toLowerCase(Locale.ROOT));
    }

    private boolean matchesKeyword(RawJobRecord record, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String title = record.rawTitle();
        return title != null
                && title.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }
}
