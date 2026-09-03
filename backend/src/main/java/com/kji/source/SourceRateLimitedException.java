package com.kji.source;

import java.time.Duration;

public class SourceRateLimitedException extends SourceException {

    private final Duration retryAfter;

    public SourceRateLimitedException(String message, Integer httpStatus, Duration retryAfter) {
        super(message, httpStatus, null);
        this.retryAfter = retryAfter;
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}
