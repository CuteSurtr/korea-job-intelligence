package com.kji.source;

import java.util.Map;

public record SourceQuery(String queryText, Map<String, String> parameters, int maxRecords) {

    public SourceQuery {
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
        maxRecords = maxRecords <= 0 ? 200 : maxRecords;
    }

    public static SourceQuery of(String queryText) {
        return new SourceQuery(queryText, Map.of(), 200);
    }

    public String parameter(String name) {
        return parameters.get(name);
    }

    public String requiredParameter(String name) {
        String value = parameters.get(name);
        if (value == null || value.isBlank()) {
            throw new SourceException("Missing required query parameter: " + name);
        }
        return value;
    }
}
