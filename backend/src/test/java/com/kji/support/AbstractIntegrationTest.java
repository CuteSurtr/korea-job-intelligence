package com.kji.support;

import com.kji.KoreaJobIntelligenceApplication;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@Tag("integration")
@SpringBootTest(classes = KoreaJobIntelligenceApplication.class)
public abstract class AbstractIntegrationTest {

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        TestDatabase database = TestDatabase.instance();
        registry.add("spring.datasource.url", database::jdbcUrl);
        registry.add("spring.datasource.username", database::username);
        registry.add("spring.datasource.password", database::password);
        registry.add("kji.internal.api-token", () -> "test-token");
        registry.add("kji.ingestion.scheduler-enabled", () -> "false");
    }
}
