package com.kji.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.kji.support.AbstractIntegrationTest;
import com.kji.support.DatabaseCleaner;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class CacheDegradationIntegrationTest extends AbstractIntegrationTest {

    private static final String NDJSON = """
            {"sourceCode":"ashby","externalId":"vessl-ai:cache-test",\
            "sourceUrl":"https://jobs.ashbyhq.com/vessl-ai/cache-test",\
            "fetchedAt":"2026-09-03T00:00:00Z","rawTitle":"Backend Engineer (Junior)",\
            "rawCompany":"VESSL AI","rawLocation":"Seoul","rawPayload":{}}""";

    @DynamicPropertySource
    static void unreachableCache(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", () -> "127.0.0.1");
        registry.add("spring.data.redis.port", () -> 6399);
        registry.add("spring.data.redis.timeout", () -> "200ms");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SearchResultCache cache;

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @BeforeEach
    void seed() throws Exception {
        databaseCleaner.clean();
        mockMvc.perform(post("/api/internal/ingestion/import")
                        .param("source", "ashby")
                        .header("X-Internal-Token", "test-token")
                        .contentType("application/x-ndjson")
                        .content(NDJSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("search still answers from PostgreSQL when the cache is unreachable")
    void searchWorksWithoutRedis() throws Exception {
        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Backend Engineer (Junior)"));

        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("an unreachable cache reports itself degraded rather than failing the request")
    void cacheReportsDegradation() {
        cache.read("probe", new TypeReference<Map<String, Object>>() {
        });

        assertThat(cache.available()).isFalse();
    }

    @Test
    @DisplayName("evicting an unreachable cache is a no-op rather than an error")
    void evictionIsSafeWhenCacheIsUnreachable() {
        cache.evictAll();
        cache.write("probe", Map.of("value", 1));

        assertThat(cache.available()).isFalse();
    }

    @Test
    @DisplayName("the health endpoint stays up when only the cache is down")
    void healthEndpointIgnoresCacheOutage() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
