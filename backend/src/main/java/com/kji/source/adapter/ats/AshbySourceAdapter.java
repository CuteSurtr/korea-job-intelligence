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
public class AshbySourceAdapter implements SourceAdapter {

    private static final String DEFAULT_BASE_URL = "https://api.ashbyhq.com";

    private final HttpJsonClient httpClient;
    private final AshbyBoardParser parser;
    private final Clock clock;
    private final String baseUrl;

    @Autowired
    public AshbySourceAdapter(HttpJsonClient httpClient, AshbyBoardParser parser, Clock clock) {
        this(httpClient, parser, clock, DEFAULT_BASE_URL);
    }

    AshbySourceAdapter(HttpJsonClient httpClient, AshbyBoardParser parser, Clock clock, String baseUrl) {
        this.httpClient = httpClient;
        this.parser = parser;
        this.clock = clock;
        this.baseUrl = baseUrl;
    }

    @Override
    public String sourceCode() {
        return AshbyBoardParser.SOURCE_CODE;
    }

    @Override
    public AdapterKind adapterKind() {
        return AdapterKind.ATS;
    }

    @Override
    public SourceFetchResult fetch(SourceQuery query) {
        String board = query.requiredParameter("board");
        String companyName = query.parameter("company");
        URI uri = URI.create(baseUrl + "/posting-api/job-board/" + board + "?includeCompensation=true");
        HttpJsonClient.JsonResponse response = httpClient.getJson(uri);
        Instant fetchedAt = Instant.now(clock);

        List<RawJobRecord> matching = parser.parse(response.body(), board, companyName, fetchedAt).stream()
                .filter(record -> matchesLocation(record, query.parameter("location")))
                .filter(record -> matchesKeyword(record, query.queryText()))
                .toList();
        List<RawJobRecord> bounded = matching.size() <= query.maxRecords()
                ? matching
                : matching.subList(0, query.maxRecords());

        return bounded.size() == matching.size()
                ? SourceFetchResult.complete(bounded, board, response.latencyMillis())
                : SourceFetchResult.partial(bounded, board, 0, response.latencyMillis());
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
        String postingId = externalKey.startsWith(board + ":")
                ? externalKey.substring(board.length() + 1)
                : externalKey;
        URI uri = URI.create(baseUrl + "/posting-api/job-board/" + board);
        try {
            HttpJsonClient.JsonResponse response = httpClient.getJson(uri);
            boolean present = parser.parse(response.body(), board, null, Instant.now(clock)).stream()
                    .anyMatch(record -> record.externalId() != null
                            && record.externalId().endsWith(":" + postingId));
            return present ? VerificationOutcome.PRESENT : VerificationOutcome.ABSENT;
        } catch (SourceException exception) {
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
