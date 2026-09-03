package com.kji.source;

import java.util.List;

public record SourceFetchResult(
        List<RawJobRecord> records,
        boolean listingComplete,
        String listingScope,
        int rateLimitEvents,
        long latencyMillis
) {

    public SourceFetchResult {
        records = records == null ? List.of() : List.copyOf(records);
        rateLimitEvents = Math.max(0, rateLimitEvents);
    }

    public static SourceFetchResult complete(List<RawJobRecord> records, String listingScope,
                                             long latencyMillis) {
        return new SourceFetchResult(records, true, listingScope, 0, latencyMillis);
    }

    public static SourceFetchResult partial(List<RawJobRecord> records, String listingScope,
                                            int rateLimitEvents, long latencyMillis) {
        return new SourceFetchResult(records, false, listingScope, rateLimitEvents, latencyMillis);
    }
}
