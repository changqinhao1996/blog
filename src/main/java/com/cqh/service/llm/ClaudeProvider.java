package com.cqh.service.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
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
 * Anthropic Claude Messages API client. Fallback provider on branch 0.0.5
 * — tried after {@link OpenAiProvider} thanks to {@code @Order(2)}.
 *
 * <p>Behaviour is identical to the pre-0.0.5 callClaudeApi method that
 * previously lived inside {@code AiServiceImpl}; this class is just an
 * extraction so the orchestrator can iterate providers uniformly.
 */
@Service
@Order(2)
public class ClaudeProvider implements LlmProvider {

    private static final String NAME = "Claude";
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String API_VERSION = "2023-06-01";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${claude.api-key:}")
    private String apiKey;

    @Value("${claude.model:claude-sonnet-4-6}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String complete(String prompt, int maxTokens) {
        if (!isConfigured()) {
            return null;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);
            headers.set("anthropic-version", API_VERSION);

            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("messages", Collections.singletonList(message));

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            logger.debug("Claude request — model: {}, prompt length: {}", model, prompt.length());
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    API_URL, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode content = root.path("content");
                if (content.isArray() && content.size() > 0) {
                    return content.get(0).path("text").asText();
                }
                logger.warn("Claude response missing content[0].text");
            }
        } catch (HttpClientErrorException e) {
            logger.warn("Claude API error ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.warn("Claude API call failed: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    @Override
    public String name() {
        return NAME;
    }
}
