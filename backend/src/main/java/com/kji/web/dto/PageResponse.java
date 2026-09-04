package com.kji.web.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    /** Wraps a list that was looked up directly rather than paged out of a repository. */
    public static <T> PageResponse<T> of(List<T> content, int size) {
        return new PageResponse<>(content, 0, size, content.size(), content.isEmpty() ? 0 : 1);
    }

    public static <S, T> PageResponse<T> from(Page<S> page, java.util.function.Function<S, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
