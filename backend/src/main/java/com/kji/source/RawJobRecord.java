package com.kji.source;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.kji.common.Hashing;
import com.kji.common.UrlCanonicalizer;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RawJobRecord(
        String sourceCode,
        String externalId,
        String sourceUrl,
        String originalApplyUrl,
        Instant fetchedAt,
        Instant postedAt,
        String rawTitle,
        String rawCompany,
        String rawLocation,
        String rawDescription,
        String rawRequirements,
        String rawEmploymentType,
        String rawExperience,
        String rawEducation,
        String rawDeadline,
        String rawRemotePolicy,
        List<String> rawSkills,
        Map<String, String> companyIdentifiers,
        String collector,
        JsonNode rawPayload
) {

    public RawJobRecord {
        rawSkills = rawSkills == null ? List.of() : List.copyOf(rawSkills);
        companyIdentifiers = companyIdentifiers == null ? Map.of() : Map.copyOf(companyIdentifiers);
    }

    public Optional<String> canonicalApplyUrl() {
        Optional<String> fromOriginal = UrlCanonicalizer.canonicalize(originalApplyUrl);
        return fromOriginal.isPresent() ? fromOriginal : UrlCanonicalizer.canonicalize(sourceUrl);
    }

    public Optional<String> canonicalUrlKey() {
        Optional<String> fromOriginal = UrlCanonicalizer.canonicalKey(originalApplyUrl);
        return fromOriginal.isPresent() ? fromOriginal : UrlCanonicalizer.canonicalKey(sourceUrl);
    }

    public String externalKey() {
        if (externalId != null && !externalId.isBlank()) {
            return externalId.trim();
        }
        return canonicalUrlKey()
                .map(key -> "url:" + Hashing.sha256(key).substring(0, 40))
                .orElseGet(() -> "content:" + contentHash().substring(0, 40));
    }

    public String contentHash() {
        return Hashing.sha256OfFields(
                sourceCode,
                rawTitle,
                rawCompany,
                rawLocation,
                rawDescription,
                rawRequirements,
                rawEmploymentType,
                rawExperience,
                rawEducation,
                rawDeadline,
                canonicalApplyUrl().orElse(null));
    }

    public String payloadHash() {
        return Hashing.sha256(rawPayload == null ? "" : rawPayload.toString());
    }

    public boolean hasMinimumFields() {
        return sourceCode != null && !sourceCode.isBlank()
                && rawTitle != null && !rawTitle.isBlank()
                && rawCompany != null && !rawCompany.isBlank()
                && fetchedAt != null;
    }
}
