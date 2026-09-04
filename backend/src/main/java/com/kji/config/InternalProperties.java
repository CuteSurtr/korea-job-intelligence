package com.kji.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kji.internal")
public record InternalProperties(String apiToken) {

    public boolean tokenConfigured() {
        return apiToken != null && !apiToken.isBlank();
    }

    /**
     * Whether a presented token is the configured one.
     *
     * <p>The comparison does not short-circuit on the first differing byte, so the time it
     * takes does not reveal how much of the token was guessed. Only the length is observable,
     * which is not worth defending here because the token is a fixed-width random string.
     */
    public boolean matches(String candidate) {
        if (!tokenConfigured() || candidate == null) {
            return false;
        }
        return MessageDigest.isEqual(
                apiToken.getBytes(StandardCharsets.UTF_8),
                candidate.getBytes(StandardCharsets.UTF_8));
    }
}
