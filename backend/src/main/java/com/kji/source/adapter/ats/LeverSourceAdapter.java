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
public class LeverSourceAdapter implements SourceAdapter {

    private static final String DEFAULT_BASE_URL = "https://api.lever.co";

    private final HttpJsonClient httpClient;
    private final LeverBoardParser parser;
    private final Clock clock;
    private final String baseUrl;

    @Autowired
    public LeverSourceAdapter(HttpJsonClient httpClient, LeverBoardParser parser, Clock clock) {
        this(httpClient, parser, clock, DEFAULT_BASE_URL);
    }

    LeverSourceAdapter(HttpJsonClient httpClient, LeverBoardParser parser, Clock clock,
                       String baseUrl) {
        this.httpClient = httpClient;
        this.parser = parser;
        this.clock = clock;
        this.baseUrl = baseUrl;
    }

    @Override
    public String sourceCode() {
        return LeverBoardParser.SOURCE_CODE;
    }

    @Override
    public AdapterKind adapterKind() {
        return AdapterKind.ATS;
    }

    @Override
    public SourceFetchResult fetch(SourceQuery query) {
        String board = query.requiredParameter("board");
        String companyName = query.parameter("company");
        URI uri = URI.create(baseUrl + "/v0/postings/" + board + "?mode=json");
        HttpJsonClient.JsonResponse response = httpClient.getJson(uri);
        Instant fetchedAt = Instant.now(clock);

        List<RawJobRecord> matching = parser.parse(response.body(), board, companyName, fetchedAt)
                .stream()
                .filter(record -> matches(record.rawLocation(), query.parameter("location")))
                .filter(record -> matches(record.rawTitle(), query.queryText()))
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
        try {
            httpClient.getJson(URI.create(baseUrl + "/v0/postings/" + board + "/" + postingId));
            return VerificationOutcome.PRESENT;
        } catch (SourceException exception) {
            Integer status = exception.httpStatus();
            if (status != null && (status == 404 || status == 410)) {
                return VerificationOutcome.ABSENT;
            }
            return VerificationOutcome.ERROR;
        }
    }

    private boolean matches(String candidate, String filter) {
        if (filter == null || filter.isBlank()) {
            return true;
        }
        return candidate != null
                && candidate.toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT));
    }
}
