package com.cqh.service;

import com.cqh.po.Blog;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    /** Larger budget for RAG answers, which need room to cite sources and explain. */
    @Value("${claude.rag-max-tokens:800}")
    private int ragMaxTokens;

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
                + blogContent;

        try {
            String response = callClaudeApi(prompt);
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
                + blogContent;

        try {
            String response = callClaudeApi(prompt);
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
    public String answerQuestion(String question, List<Blog> contextBlogs) {
        if (!isConfigured()) {
            logger.warn("Claude API key not configured, skipping RAG answer");
            return null;
        }
        if (question == null || question.trim().isEmpty()) return null;
        if (contextBlogs == null || contextBlogs.isEmpty()) return null;

        StringBuilder context = new StringBuilder();
        for (int i = 0; i < contextBlogs.size(); i++) {
            Blog b = contextBlogs.get(i);
            String title = b.getTitle() == null ? "(untitled)" : b.getTitle();
            String content = b.getContent() == null ? "" : b.getContent();
            context.append("--- Source [").append(i + 1).append("]: ")
                   .append(title).append(" ---\n")
                   .append(content).append("\n\n");
        }

        String prompt =
                "You answer questions using ONLY the blog posts supplied below as context. "
              + "Cite the sources you use with bracketed numbers like [1] or [2,3] that match "
              + "the source numbers in the context. If the posts do not contain the answer, "
              + "say so plainly and do not invent facts.\n\n"
              + context.toString()
              + "Question: " + question.trim() + "\n\nAnswer:";

        try {
            String response = callClaudeApi(prompt, ragMaxTokens);
            if (response != null && !response.trim().isEmpty()) {
                logger.info("RAG answer generated ({} chars, {} sources)",
                        response.length(), contextBlogs.size());
                return response.trim();
            }
        } catch (Exception e) {
            logger.warn("Failed to generate RAG answer: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Call the Claude Messages API and return the text content of the first response block.
     * Uses the default {@code claude.max-tokens} budget.
     */
    String callClaudeApi(String userMessage) {
        return callClaudeApi(userMessage, maxTokens);
    }

    /**
     * Call the Claude Messages API with an explicit max-tokens budget — used by
     * {@link #answerQuestion} so RAG answers have room to cite and explain.
     */
    String callClaudeApi(String userMessage, int maxTokensForCall) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", API_VERSION);

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", userMessage);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("max_tokens", maxTokensForCall);
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
