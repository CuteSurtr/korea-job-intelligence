package com.kji.ingest;

public record MalformedLine(String line, String reasonCode, String message) {
}
