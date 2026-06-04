package com.cqh.service;

import com.cqh.config.AiGuardrailProperties;
import com.cqh.util.PromptGuardrails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Application-level guardrail decorator around {@link AiServiceImpl}.
 *
 * <p>Marked {@link Primary} so existing injection points ({@code BlogServiceImpl},
 * {@code BlogController}) transparently pick it up without any change to them.
 * For each call it: rate-limits, truncates the untrusted input, delegates, then
 * sanitizes generated text. When {@code claude.guardrail.enabled} is false the
 * decorator is a pure pass-through, preserving the original behaviour exactly.</p>
 */
@Service
@Primary
public class GuardedAiService implements AiService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private AiServiceImpl delegate;

    @Autowired
    private AiGuardrailProperties props;

    @Autowired
    private RateLimiter rateLimiter;

    @Override
    public String generateSummary(String blogContent) {
        if (!props.isEnabled()) {
            return delegate.generateSummary(blogContent);
        }
        if (!rateLimiter.tryAcquire("summary", props.getRateLimitPerMinute())) {
            logger.warn("AI rate limit exceeded, skipping summary generation");
            return null;
        }
        String safe = PromptGuardrails.truncate(blogContent, props.getMaxInputChars());
        String result = delegate.generateSummary(safe);
        return blankToNull(PromptGuardrails.sanitizeOutput(result, props.getMaxOutputChars()));
    }

    @Override
    public String generateDescription(String blogContent) {
        if (!props.isEnabled()) {
            return delegate.generateDescription(blogContent);
        }
        if (!rateLimiter.tryAcquire("description", props.getRateLimitPerMinute())) {
            logger.warn("AI rate limit exceeded, skipping description generation");
            return null;
        }
        String safe = PromptGuardrails.truncate(blogContent, props.getMaxInputChars());
        String result = delegate.generateDescription(safe);
        return blankToNull(PromptGuardrails.sanitizeOutput(result, props.getMaxOutputChars()));
    }

    @Override
    public List<String> suggestTagNames(String blogContent, List<String> existingTagNames) {
        if (!props.isEnabled()) {
            return delegate.suggestTagNames(blogContent, existingTagNames);
        }
        if (!rateLimiter.tryAcquire("tags", props.getRateLimitPerMinute())) {
            logger.warn("AI rate limit exceeded, skipping tag suggestion");
            return Collections.emptyList();
        }
        String safe = PromptGuardrails.truncate(blogContent, props.getMaxInputChars());
        // Output is already validated against the existing tag list by the delegate,
        // so no extra output sanitisation is required here.
        return delegate.suggestTagNames(safe, existingTagNames);
    }

    private static String blankToNull(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s;
    }
}
