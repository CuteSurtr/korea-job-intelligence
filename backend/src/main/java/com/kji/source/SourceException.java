package com.kji.source;

public class SourceException extends RuntimeException {

    private final Integer httpStatus;

    public SourceException(String message) {
        this(message, null, null);
    }

    public SourceException(String message, Throwable cause) {
        this(message, null, cause);
    }

    public SourceException(String message, Integer httpStatus, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    public Integer httpStatus() {
        return httpStatus;
    }
}
