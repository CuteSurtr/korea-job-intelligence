package com.kji.normalize;

import java.util.Optional;

public record Extracted<T>(T value, double confidence, String evidence, ExtractionMethod method) {

    public static <T> Extracted<T> unknown() {
        return new Extracted<>(null, 0.0d, null, ExtractionMethod.HEURISTIC);
    }

    public static <T> Extracted<T> unknown(String evidence) {
        return new Extracted<>(null, 0.0d, evidence, ExtractionMethod.HEURISTIC);
    }

    public static <T> Extracted<T> of(T value, double confidence, String evidence, ExtractionMethod method) {
        return new Extracted<>(value, confidence, evidence, method);
    }

    public boolean isKnown() {
        return value != null;
    }

    public Optional<T> asOptional() {
        return Optional.ofNullable(value);
    }

    public enum ExtractionMethod {
        SOURCE_STRUCTURED,
        PATTERN_MATCH,
        LEXICON,
        HEURISTIC,
        MANUAL
    }
}
