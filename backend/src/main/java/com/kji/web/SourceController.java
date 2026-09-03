package com.kji.web;

import com.kji.source.SourceAdapterRegistry;
import com.kji.source.SourceHealthService;
import com.kji.source.SourceRepository;
import com.kji.web.dto.SourceResponse;
import java.util.Comparator;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sources")
public class SourceController {

    private final SourceRepository sourceRepository;
    private final SourceHealthService healthService;
    private final SourceAdapterRegistry adapterRegistry;

    public SourceController(SourceRepository sourceRepository,
                            SourceHealthService healthService,
                            SourceAdapterRegistry adapterRegistry) {
        this.sourceRepository = sourceRepository;
        this.healthService = healthService;
        this.adapterRegistry = adapterRegistry;
    }

    @GetMapping
    public List<SourceResponse> list() {
        return sourceRepository.findAll().stream()
                .sorted(Comparator.comparing(source -> source.getCode()))
                .map(source -> SourceResponse.from(source,
                        adapterRegistry.find(source.getCode()).isPresent()))
                .toList();
    }

    @GetMapping("/health")
    public List<SourceResponse.Health> health() {
        return healthService.all().stream()
                .map(SourceResponse.Health::from)
                .toList();
    }
}
