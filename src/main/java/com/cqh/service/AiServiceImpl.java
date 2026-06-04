package com.cqh.service;

import com.cqh.config.AiGuardrailProperties;
import com.cqh.util.PromptGuardrails;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class AiServiceImpl implements AiService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String API_VERSION = "2023-06-01";

    @Value("${claude.api-key:}")
    private String apiKey;

    @Value("${claude.model:claude-sonnet-4-6}")
    private String model;

    @Value("${claude.max-tokens:300}")
    private int maxTokens;

    // Optional: absent in plain unit tests that construct this class directly.
    // When null or disabled, prompts fall back to their original un-guarded form.
    @Autowired(required = false)
    private AiGuardrailProperties guardrailProperties;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String generateSummary(String blogContent) {
        if (!isConfigured()) {
            logger.warn("Claude API key not configured, skipping summary generation");
            return null;
        }
        if (blogContent == null || blogContent.trim().isEmpty()) {
            return null;
        }

        String prompt = "Summarize the following blog post in 2-3 concise sentences. "
                + "Return only the summary text, no extra formatting or labels.\n\n"
                + content(blogContent);

        try {
            String response = callClaudeApi(systemPrompt(PromptGuardrails.SUMMARY_SYSTEM_PROMPT), prompt);
            if (response != null && !response.trim().isEmpty()) {
                logger.info("AI summary generated successfully");
                return response.trim();
            }
        } catch (Exception e) {
            logger.warn("Failed to generate AI summary: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public List<String> suggestTagNames(String blogContent, List<String> existingTagNames) {
        if (!isConfigured()) {
            logger.warn("Claude API key not configured, skipping tag suggestion");
            return Collections.emptyList();
        }
        if (blogContent == null || blogContent.trim().isEmpty() || existingTagNames == null || existingTagNames.isEmpty()) {
            return Collections.emptyList();
        }

        String tagList = String.join(", ", existingTagNames);
        String prompt = "Given the following blog post, select 1-5 relevant tags from this list: ["
                + tagList + "]\n\n"
                + "Return ONLY the selected tag names separated by commas, nothing else. "
                + "Only pick tags from the provided list.\n\n"
                + content(blogContent);

        try {
            String response = callClaudeApi(systemPrompt(PromptGuardrails.TAG_SYSTEM_PROMPT), prompt);
            if (response != null && !response.trim().isEmpty()) {
                List<String> suggested = new ArrayList<>();
                for (String name : response.split(",")) {
                    String trimmed = name.trim();
                    if (!trimmed.isEmpty() && existingTagNames.contains(trimmed)) {
                        suggested.add(trimmed);
                    }
                }
                logger.info("AI suggested {} tags: {}", suggested.size(), suggested);
                return suggested;
            }
        } catch (Exception e) {
            logger.warn("Failed to suggest tags: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    @Override
    public String generateDescription(String blogContent) {
        if (!isConfigured()) {
            logger.warn("Claude API key not configured, skipping description generation");
            return null;
        }
        if (blogContent == null || blogContent.trim().isEmpty()) {
            return null;
        }

        String prompt = "Write a single concise sentence (under 200 characters) "
                + "that describes what the following blog post is about. "
                + "It will be shown as preview text on list pages, so make it "
                + "engaging and informative. Return ONLY the sentence — no "
                + "quotes, no labels, no markdown.\n\n"
                + content(blogContent);

        try {
            String response = callClaudeApi(systemPrompt(PromptGuardrails.DESCRIPTION_SYSTEM_PROMPT), prompt);
            if (response != null && !response.trim().isEmpty()) {
                String cleaned = response.trim();
                // Belt-and-braces: the DB column may be VARCHAR(200), so cap it.
                if (cleaned.length() > 200) {
                    cleaned = cleaned.substring(0, 197) + "...";
                }
                logger.info("AI description generated successfully ({} chars)", cleaned.length());
                return cleaned;
            }
        } catch (Exception e) {
            logger.warn("Failed to generate AI description: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Whether the guardrail layer is active. False when the properties bean is
     * absent (plain unit tests) or explicitly disabled — in which case prompts
     * keep their original, un-guarded form.
     */
    private boolean guardrailsOn() {
        return guardrailProperties != null && guardrailProperties.isEnabled();
    }

    /** Wrap untrusted content in delimiters when guardrails are on, else pass through. */
    private String content(String blogContent) {
        return guardrailsOn() ? PromptGuardrails.wrapUntrusted(blogContent) : blogContent;
    }

    /** Return the given system prompt when guardrails are on, else null (no system field). */
    private String systemPrompt(String prompt) {
        return guardrailsOn() ? prompt : null;
    }

    /**
     * Call the Claude Messages API and return the text content of the first response block.
     */
    String callClaudeApi(String userMessage) {
        return callClaudeApi(null, userMessage);
    }

    /**
     * Call the Claude Messages API with an optional system prompt. When
     * {@code systemPrompt} is null/blank no {@code system} field is sent, so the
     * request is identical to the legacy single-argument form.
     */
    String callClaudeApi(String systemPrompt, String userMessage) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", API_VERSION);

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", userMessage);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("max_tokens", maxTokens);
        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            requestBody.put("system", systemPrompt);
        }
        requestBody.put("messages", Collections.singletonList(message));

        try {
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            logger.debug("Claude API request - model: {}, content length: {}", model, userMessage.length());
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    API_URL, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode content = root.path("content");
                if (content.isArray() && content.size() > 0) {
                    return content.get(0).path("text").asText();
                }
            }
        } catch (HttpClientErrorException e) {
            logger.warn("Claude API error ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Claude API call failed", e);
        } catch (Exception e) {
            logger.warn("Claude API call failed: {}", e.getMessage());
            throw new RuntimeException("Claude API call failed", e);
        }
        return null;
    }

    boolean isConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }
}
