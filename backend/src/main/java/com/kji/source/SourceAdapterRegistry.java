package com.kji.source;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class SourceAdapterRegistry {

    private final Map<String, SourceAdapter> adaptersByCode = new LinkedHashMap<>();

    public SourceAdapterRegistry(List<SourceAdapter> adapters) {
        for (SourceAdapter adapter : adapters) {
            SourceAdapter previous = adaptersByCode.put(adapter.sourceCode(), adapter);
            if (previous != null) {
                throw new IllegalStateException(
                        "Duplicate adapter registered for source code " + adapter.sourceCode());
            }
        }
    }

    public Optional<SourceAdapter> find(String sourceCode) {
        return Optional.ofNullable(adaptersByCode.get(sourceCode));
    }

    public SourceAdapter require(String sourceCode) {
        return find(sourceCode).orElseThrow(() ->
                new SourceException("No adapter registered for source code " + sourceCode));
    }

    public List<String> registeredCodes() {
        return List.copyOf(adaptersByCode.keySet());
    }
}
