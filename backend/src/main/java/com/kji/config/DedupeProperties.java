package com.kji.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kji.dedupe")
public record DedupeProperties(
        double autoMergeThreshold,
        double reviewThreshold,
        double descriptionSimilarityThreshold,
        int minDescriptionLength
) {

    public DedupeProperties {
        autoMergeThreshold = autoMergeThreshold <= 0 ? 0.75 : autoMergeThreshold;
        reviewThreshold = reviewThreshold <= 0 ? 0.60 : reviewThreshold;
        descriptionSimilarityThreshold =
                descriptionSimilarityThreshold <= 0 ? 0.82 : descriptionSimilarityThreshold;
        minDescriptionLength = minDescriptionLength <= 0 ? 400 : minDescriptionLength;
    }
}
