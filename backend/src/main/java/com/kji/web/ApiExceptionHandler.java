package com.kji.web;

import com.kji.company.CompanyResolutionException;
import com.kji.crm.ApplicationService;
import com.kji.ingest.ImportService;
import com.kji.source.SourceException;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(SourceException.class)
    public ResponseEntity<Map<String, Object>> handleSource(SourceException exception) {
        return body(HttpStatus.BAD_REQUEST, "source_error", exception.getMessage());
    }

    @ExceptionHandler(CompanyResolutionException.class)
    public ResponseEntity<Map<String, Object>> handleCompany(CompanyResolutionException exception) {
        return body(HttpStatus.BAD_REQUEST, "company_resolution_error", exception.getMessage());
    }

    @ExceptionHandler(ImportService.ImportTooLargeException.class)
    public ResponseEntity<Map<String, Object>> handleImportTooLarge(
            ImportService.ImportTooLargeException exception) {
        return body(HttpStatus.PAYLOAD_TOO_LARGE, "import_too_large", exception.getMessage());
    }

    @ExceptionHandler(ApplicationService.ApplicationNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleApplicationNotFound(
            ApplicationService.ApplicationNotFoundException exception) {
        return body(HttpStatus.NOT_FOUND, "not_found", exception.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException exception) {
        return body(HttpStatus.NOT_FOUND, "not_found", exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException exception) {
        return body(HttpStatus.BAD_REQUEST, "invalid_request", exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception exception) {
        log.error("unhandled request failure", exception);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", "Unexpected server error");
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String error, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "error", error,
                "message", message == null ? "" : message,
                "timestamp", Instant.now().toString()));
    }
}
