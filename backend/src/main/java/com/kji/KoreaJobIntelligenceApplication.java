package com.kji;

import com.kji.config.CacheProperties;
import com.kji.config.DedupeProperties;
import com.kji.config.IngestionProperties;
import com.kji.config.InternalProperties;
import com.kji.config.SourceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
        SourceProperties.class,
        IngestionProperties.class,
        CacheProperties.class,
        DedupeProperties.class,
        InternalProperties.class
})
public class KoreaJobIntelligenceApplication {

    public static void main(String[] args) {
        SpringApplication.run(KoreaJobIntelligenceApplication.class, args);
    }
}
