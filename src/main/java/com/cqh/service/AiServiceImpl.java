package com.cqh.service;

import com.cqh.po.Blog;
import com.cqh.service.llm.LlmProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AI service for blog summaries, tag suggestions, descriptions, and RAG
 * answers. Does NOT speak any vendor protocol directly — instead it builds
 * the prompt for each task and hands it off to the configured chain of
 * {@link LlmProvider} beans.
 *
 * <p>Branch 0.0.5: providers are tried in {@code @Order} sequence —
 * {@code OpenAiProvider} (Order 1) first, then {@code ClaudeProvider}
 * (Order 2) as fallback. The first provider to return a non-empty response
 * wins. Unconfigured providers (missing API key) are skipped silently.
 *
 * <p>Graceful degradation: if no provider succeeds, the public methods
 * return {@code null} (or an empty list for {@code suggestTagNames}), and
 * callers (BlogServiceImpl, BlogController, RagService) treat that as
 * "AI not available" without throwing.
 */
@Service
public class AiServiceImpl implements AiService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${claude.max-tokens:300}")
    private int maxTokens;

    /** Larger budget for RAG answers, which need room to cite sources and explain. */
    @Value("${claude.rag-max-tokens:800}")
    private int ragMaxTokens;

    /**
     * All LlmProvider beans, in {@code @Order} sequence. On 0.0.5 this is
     * [OpenAiProvider, ClaudeProvider]; future providers can be added by
     * dropping a new {@code @Service @Order(N)} bean — no change here.
     */
    @Autowired
    private List<LlmProvider> providers;

    @Override
    public String generateSummary(String blogContent) {
        if (!isConfigured()) {
            logger.warn("No AI provider configured, skipping summary generation");
            return null;
        }
        if (blogContent == null || blogContent.trim().isEmpty()) {
            return null;
        }

        String prompt = "Summarize the following blog post in 2-3 concise sentences. "
                + "Return only the summary text, no extra formatting or labels.\n\n"
                + blogContent;

        try {
            String response = callLlm(prompt, maxTokens);
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
            logger.warn("No AI provider configured, skipping tag suggestion");
            return Collections.emptyList();
        }
        if (blogContent == null || blogContent.trim().isEmpty()
                || existingTagNames == null || existingTagNames.isEmpty()) {
            return Collections.emptyList();
        }

        String tagList = String.join(", ", existingTagNames);
        String prompt = "Given the following blog post, select 1-5 relevant tags from this list: ["
                + tagList + "]\n\n"
                + "Return ONLY the selected tag names separated by commas, nothing else. "
                + "Only pick tags from the provided list.\n\n"
                + blogContent;

        try {
            String response = callLlm(prompt, maxTokens);
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
            logger.warn("No AI provider configured, skipping RAG answer");
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
            String response = callLlm(prompt, ragMaxTokens);
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
     * Iterate the configured providers in {@code @Order} sequence, returning
     * the first non-empty response. Returns null if every provider is either
     * unconfigured, errors, or returns an empty string.
     */
    String callLlm(String prompt, int maxTokensForCall) {
        if (providers == null) return null;
        for (LlmProvider provider : providers) {
            if (!provider.isConfigured()) {
                logger.debug("Provider {} not configured, skipping", provider.name());
                continue;
            }
            try {
                String response = provider.complete(prompt, maxTokensForCall);
                if (response != null && !response.trim().isEmpty()) {
                    logger.info("AI response from {} ({} chars)", provider.name(), response.length());
                    return response;
                }
                logger.warn("{} returned empty response; trying next provider", provider.name());
            } catch (Exception e) {
                logger.warn("{} call failed: {}; trying next provider", provider.name(), e.getMessage());
            }
        }
        logger.warn("All AI providers failed or are not configured");
        return null;
    }

    /**
     * @return true if at least one provider has been configured (has an API
     * key set). Used as a fast-path guard before building prompts.
     */
    boolean isConfigured() {
        if (providers == null) return false;
        for (LlmProvider p : providers) {
            if (p.isConfigured()) return true;
        }
        return false;
    }
}
