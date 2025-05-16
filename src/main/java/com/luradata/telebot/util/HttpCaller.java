package com.luradata.telebot.util;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class HttpCaller {
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public HttpCaller() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(DEFAULT_TIMEOUT)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public <T> CompletableFuture<T> callApi(HttpRequestConfig config, Class<T> responseType) {
        try {
            HttpRequest httpRequest = buildHttpRequest(config);
            
            return httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            try {
                                return objectMapper.readValue(response.body(), responseType);
                            } catch (Exception e) {
                                log.error("Error parsing response: {}", response.body(), e);
                                throw new RuntimeException("Failed to parse response", e);
                            }
                        } else {
                            log.error("Error calling API. Status code: {}, Response: {}", 
                                    response.statusCode(), response.body());
                            throw new RuntimeException("API call failed with status: " + response.statusCode());
                        }
                    })
                    .exceptionally(ex -> {
                        log.error("Exception while calling API", ex);
                        throw new RuntimeException("API call failed", ex);
                    });

        } catch (Exception e) {
            log.error("Error preparing API request", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    public <T> void streamApiResponse(HttpRequestConfig config, Consumer<T> responseConsumer, Class<T> responseType) {
        try {
            HttpRequest httpRequest = buildHttpRequest(config);

            httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofLines())
                    .thenAccept(response -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            response.body().forEach(line -> {
                                try {
                                    if (!line.trim().isEmpty()) {
                                        T parsedResponse = objectMapper.readValue(line, responseType);
                                        responseConsumer.accept(parsedResponse);
                                    }
                                } catch (Exception e) {
                                    log.error("Error parsing response line: {}", line, e);
                                }
                            });
                        } else {
                            log.error("Error calling API. Status code: {}", response.statusCode());
                            throw new RuntimeException("API call failed with status: " + response.statusCode());
                        }
                    })
                    .exceptionally(ex -> {
                        log.error("Exception while calling API", ex);
                        throw new RuntimeException("API call failed", ex);
                    });

        } catch (Exception e) {
            log.error("Error preparing API request", e);
            throw new RuntimeException("Failed to prepare API request", e);
        }
    }

    HttpRequest buildHttpRequest(HttpRequestConfig config) throws IOException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(config.getUrl()))
                .timeout(config.getTimeout() != null ? config.getTimeout() : DEFAULT_TIMEOUT);

        // Add headers
        if (config.getHeaders() != null) {
            config.getHeaders().forEach(requestBuilder::header);
        }

        // Set method and body
        switch (config.getMethod()) {
            case GET:
                requestBuilder.GET();
                break;
            case POST:
                requestBuilder.POST(HttpRequest.BodyPublishers.ofString(
                    config.getBody() != null ? objectMapper.writeValueAsString(config.getBody()) : ""));
                break;
            case PUT:
                requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(
                    config.getBody() != null ? objectMapper.writeValueAsString(config.getBody()) : ""));
                break;
            case DELETE:
                requestBuilder.DELETE();
                break;
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + config.getMethod());
        }

        return requestBuilder.build();
    }

    @Data
    @Builder
    public static class HttpRequestConfig {
        private String url;
        private HttpMethod method;
        private Map<String, String> headers;
        private Object body;
        private Duration timeout;
    }

    public enum HttpMethod {
        GET, POST, PUT, DELETE
    }
}