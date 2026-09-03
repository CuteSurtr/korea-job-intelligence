package com.kji.source;

import java.util.List;

public record SourceFetchResult(
        List<RawJobRecord> records,
        boolean listingComplete,
        int rateLimitEvents,
        long latencyMillis
) {

    public SourceFetchResult {
        records = records == null ? List.of() : List.copyOf(records);
        rateLimitEvents = Math.max(0, rateLimitEvents);
    }

    public static SourceFetchResult complete(List<RawJobRecord> records, long latencyMillis) {
        return new SourceFetchResult(records, true, 0, latencyMillis);
    }

    public static SourceFetchResult partial(List<RawJobRecord> records, int rateLimitEvents, long latencyMillis) {
        return new SourceFetchResult(records, false, rateLimitEvents, latencyMillis);
    }
}
