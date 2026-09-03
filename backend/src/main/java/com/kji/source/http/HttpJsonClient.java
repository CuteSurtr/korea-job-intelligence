package com.kji.source.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kji.config.SourceProperties;
import com.kji.source.SourceException;
import com.kji.source.SourceRateLimitedException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class HttpJsonClient {

    private final ObjectMapper objectMapper;
    private final SourceProperties properties;
    private final HttpClient httpClient;

    public HttpJsonClient(ObjectMapper objectMapper, SourceProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.requestTimeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public JsonResponse getJson(URI uri) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(properties.requestTimeout())
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.ACCEPT_LANGUAGE, "ko-KR,ko;q=0.9,en;q=0.8")
                .header(HttpHeaders.USER_AGENT, properties.userAgent())
                .GET()
                .build();
        return send(request);
    }

    private JsonResponse send(HttpRequest request) {
        int attempts = properties.maxRetries() + 1;
        long startedNanos = System.nanoTime();
        SourceException lastFailure = null;

        for (int attempt = 0; attempt < attempts; attempt++) {
            try {
                HttpResponse<String> response =
                        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                long latency = elapsedMillis(startedNanos);
                int status = response.statusCode();

                if (status >= 200 && status < 300) {
                    return new JsonResponse(status, parse(response.body()), latency);
                }
                if (status == 429) {
                    throw new SourceRateLimitedException(
                            "Rate limited by source", status, retryAfter(response).orElse(null));
                }
                if (status == 404 || status == 410) {
                    throw new SourceException("Resource not found", status, null);
                }
                lastFailure = new SourceException(
                        "Source returned HTTP " + status + ": " + truncate(response.body()), status, null);
                if (status < 500) {
                    throw lastFailure;
                }
            } catch (SourceRateLimitedException exception) {
                throw exception;
            } catch (SourceException exception) {
                throw exception;
            } catch (IOException exception) {
                lastFailure = new SourceException("Source request failed: " + exception.getMessage(), exception);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new SourceException("Source request interrupted", exception);
            }

            if (attempt < attempts - 1) {
                backoff(attempt);
            }
        }
        throw lastFailure == null ? new SourceException("Source request failed") : lastFailure;
    }

    private JsonNode parse(String body) {
        try {
            return objectMapper.readTree(body == null || body.isBlank() ? "{}" : body);
        } catch (IOException exception) {
            throw new SourceException("Source response was not valid JSON", exception);
        }
    }

    private Optional<Duration> retryAfter(HttpResponse<String> response) {
        return response.headers().firstValue("retry-after").flatMap(value -> {
            try {
                return Optional.of(Duration.ofSeconds(Long.parseLong(value.trim())));
            } catch (NumberFormatException exception) {
                return Optional.empty();
            }
        });
    }

    private void backoff(int attempt) {
        long base = properties.initialBackoff().toMillis();
        long delay = Math.min(properties.maxBackoff().toMillis(), base * (1L << Math.min(attempt, 10)));
        try {
            Thread.sleep(delay);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new SourceException("Retry backoff interrupted", exception);
        }
    }

    private long elapsedMillis(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000L;
    }

    private String truncate(String body) {
        if (body == null) {
            return "";
        }
        return body.length() <= 200 ? body : body.substring(0, 200);
    }

    public record JsonResponse(int status, JsonNode body, long latencyMillis) {
    }
}
