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

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> body = new HashMap<>();
            body.put("input", Collections.singletonList(text));
            body.put("model", model);

            String jsonBody = objectMapper.writeValueAsString(body);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

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
            }
        } catch (Exception e) {
            logger.warn("Embedding call failed: {}", e.getMessage());
        }
        return null;
    }

    boolean isConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }
}
