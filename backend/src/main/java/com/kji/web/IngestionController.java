package com.kji.web;

import com.kji.ingest.ImportService;
import com.kji.ingest.IngestionOutcome;
import com.kji.ingest.IngestionPipeline;
import com.kji.ingest.SearchRun;
import com.kji.source.SourceQuery;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/ingestion")
public class IngestionController {

    private final ImportService importService;
    private final IngestionPipeline pipeline;

    public IngestionController(ImportService importService, IngestionPipeline pipeline) {
        this.importService = importService;
        this.pipeline = pipeline;
    }

    @PostMapping(value = "/import", consumes = {"application/x-ndjson", MediaType.TEXT_PLAIN_VALUE,
            MediaType.APPLICATION_OCTET_STREAM_VALUE})
    public IngestionOutcome importNdjson(@RequestParam("source") String sourceCode,
                                         @RequestParam(value = "collector", required = false) String collector,
                                         HttpServletRequest request) throws IOException {
        return importService.importNdjson(sourceCode, request.getInputStream(), collector);
    }

    @PostMapping(value = "/run", consumes = MediaType.APPLICATION_JSON_VALUE)
    public IngestionOutcome run(@RequestBody RunRequest body) {
        Map<String, String> parameters = new HashMap<>(
                body.parameters() == null ? Map.of() : body.parameters());
        SourceQuery query = new SourceQuery(body.query(), parameters,
                body.maxRecords() == null ? 200 : body.maxRecords());
        return pipeline.runDirect(body.source(), query, SearchRun.TriggerKind.MANUAL);
    }

    public record RunRequest(String source, String query, Map<String, String> parameters,
                             Integer maxRecords) {
    }
}
