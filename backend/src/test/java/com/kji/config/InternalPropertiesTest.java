package com.kji.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InternalPropertiesTest {

    private static final String TOKEN = "5f2b8c1d4e6a7b9c0d1e2f3a4b5c6d7e";

    @Test
    @DisplayName("an instance with no token configured has its internal endpoints disabled")
    void unconfiguredTokenDisablesTheEndpoints() {
        assertThat(new InternalProperties(null).tokenConfigured()).isFalse();
        assertThat(new InternalProperties("").tokenConfigured()).isFalse();
        assertThat(new InternalProperties("   ").tokenConfigured()).isFalse();
    }

    @Test
    @DisplayName("nothing matches when no token is configured, not even an empty presentation")
    void unconfiguredTokenMatchesNothing() {
        InternalProperties properties = new InternalProperties(null);

        assertThat(properties.matches(null)).isFalse();
        assertThat(properties.matches("")).isFalse();
        assertThat(properties.matches(TOKEN)).isFalse();
    }

    @Test
    @DisplayName("only the configured token matches")
    void theConfiguredTokenMatches() {
        InternalProperties properties = new InternalProperties(TOKEN);

        assertThat(properties.matches(TOKEN)).isTrue();
        assertThat(properties.matches(null)).isFalse();
        assertThat(properties.matches("")).isFalse();
        // a near miss must not pass, whether it differs at the end, the start, or in length
        assertThat(properties.matches(TOKEN.substring(0, TOKEN.length() - 1) + "f")).isFalse();
        assertThat(properties.matches("a" + TOKEN.substring(1))).isFalse();
        assertThat(properties.matches(TOKEN.substring(0, TOKEN.length() - 1))).isFalse();
        assertThat(properties.matches(TOKEN + "extra")).isFalse();
        // a prefix of the token is the shape a byte-at-a-time guess would take
        assertThat(properties.matches(TOKEN.substring(0, 4))).isFalse();
    }

    @Test
    @DisplayName("the comparison is over bytes, so a token is not matched case-insensitively")
    void matchingIsExact() {
        InternalProperties properties = new InternalProperties(TOKEN);

        assertThat(properties.matches(TOKEN.toUpperCase())).isFalse();
        assertThat(properties.matches(" " + TOKEN)).isFalse();
        assertThat(properties.matches(TOKEN + " ")).isFalse();
    }
}
