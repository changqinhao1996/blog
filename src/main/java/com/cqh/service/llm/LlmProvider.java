package com.cqh.service.llm;

/**
 * Tiny abstraction over a single-turn chat completion call.
 *
 * <p>Implementations wrap a specific vendor's HTTP API (OpenAI, Anthropic Claude,
 * etc.). They return the model's text response on success and {@code null} on
 * any failure — so that {@link com.cqh.service.AiServiceImpl} can iterate
 * configured providers and try the next one cleanly.
 *
 * <p>The iteration order is controlled by Spring's {@code @Order} annotation on
 * the implementing bean: lower values are tried first. Branch 0.0.5 wires
 * OpenAI as {@code @Order(1)} and Claude as {@code @Order(2)}, so OpenAI is
 * the primary provider and Claude is the fallback.
 */
public interface LlmProvider {

    /**
     * Send a single user prompt to the model and return the response text.
     *
     * @param prompt    the full user prompt (already assembled by the caller —
     *                  this method does no further formatting)
     * @param maxTokens upper bound on the response length, in tokens
     * @return the model's response text, or {@code null} on any failure
     *         (missing API key, HTTP error, malformed response, etc.)
     */
    String complete(String prompt, int maxTokens);

    /**
     * @return true if this provider has been configured (e.g. its API key is
     * present in the environment). Unconfigured providers are skipped without
     * any HTTP call.
     */
    boolean isConfigured();

    /** Short human-readable provider name, used in log lines. */
    String name();
}
