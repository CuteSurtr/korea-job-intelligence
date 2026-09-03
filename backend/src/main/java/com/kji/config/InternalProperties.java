package com.kji.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kji.internal")
public record InternalProperties(String apiToken) {

    public boolean tokenConfigured() {
        return apiToken != null && !apiToken.isBlank();
    }

    public boolean matches(String candidate) {
        return tokenConfigured() && apiToken.equals(candidate);
    }
}
