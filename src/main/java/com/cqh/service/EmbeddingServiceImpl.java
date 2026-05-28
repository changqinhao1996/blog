package com.cqh.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Voyage AI embedding client. Mirrors {@link AiServiceImpl} in shape:
 * - reads configuration via @Value
 * - holds its own RestTemplate
 * - never throws — returns null on any failure so callers degrade gracefully
 */
@Service
public class EmbeddingServiceImpl implements EmbeddingService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String API_URL = "https://api.voyageai.com/v1/embeddings";

    @Value("${voyage.api-key:}")
    private String apiKey;

    @Value("${voyage.model:voyage-3-lite}")
    private String model;

    /** How many times to attempt an embedding call before giving up. */
    @Value("${voyage.max-retries:5}")
    private int maxRetries;

    /** Base backoff (ms) applied (linearly) when the API returns HTTP 429. */
    @Value("${voyage.retry-backoff-ms:20000}")
    private long retryBackoffMs;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String embed(String text) {
        if (!isConfigured()) {
            logger.warn("Voyage API key not configured, skipping embedding");
            return null;
        }
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        String jsonBody;
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("input", Collections.singletonList(text));
            body.put("model", model);
            jsonBody = objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            logger.warn("Embedding request build failed: {}", e.getMessage());
            return null;
        }
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        int attempts = Math.max(1, maxRetries);
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        API_URL, HttpMethod.POST, entity, String.class);

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    JsonNode arr = root.path("data").path(0).path("embedding");
                    if (arr.isArray() && arr.size() > 0) {
                        // Jackson's toString() emits "[0.1,0.2,...]" — exactly the
                        // format MySQL's STRING_TO_VECTOR() expects.
                        String vec = arr.toString();
                        logger.info("Embedding generated (dim={})", arr.size());
                        return vec;
                    }
                    logger.warn("Voyage response missing embedding array");
                    return null;
                }
            } catch (HttpClientErrorException e) {
                // Voyage's free tier is rate-limited (~3 req/min). Back off and retry
                // on 429; any other 4xx is fatal for this call.
                if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS && attempt < attempts) {
                    long wait = retryBackoffMs * attempt; // linear backoff
                    logger.warn("Voyage 429 (attempt {}/{}), backing off {} ms",
                            attempt, attempts, wait);
                    try {
                        Thread.sleep(wait);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                    continue;
                }
                logger.warn("Embedding call failed: {} {}", e.getStatusCode(), e.getMessage());
                return null;
            } catch (Exception e) {
                logger.warn("Embedding call failed: {}", e.getMessage());
                return null;
            }
        }
        logger.warn("Embedding failed after {} attempts (rate limited)", attempts);
        return null;
    }

    boolean isConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }
}
