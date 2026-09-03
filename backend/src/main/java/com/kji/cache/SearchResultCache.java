package com.kji.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Optional;

public interface SearchResultCache {

    <T> Optional<T> read(String key, TypeReference<T> type);

    void write(String key, Object value);

    void evictAll();

    boolean available();
}
