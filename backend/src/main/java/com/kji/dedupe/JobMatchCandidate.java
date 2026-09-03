package com.kji.dedupe;

public record JobMatchCandidate(
        Long companyId,
        Long sourceId,
        boolean sourceHasStableExternalId,
        String externalKey,
        String canonicalUrlKey,
        String normalizedTitle,
        String normalizedDescription,
        String locationCity
) {
}
