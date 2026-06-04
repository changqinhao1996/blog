package com.cqh.service;

import com.cqh.config.AiGuardrailProperties;
import com.cqh.util.PromptGuardrails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Claude-backed {@link ModerationService}. Reuses the configured HTTP client of
 * {@link AiServiceImpl} so there is a single place that talks to the API.
 *
 * <p><strong>Fail-closed:</strong> unlike the convenience features (summary /
 * tags / description) which fail open, moderation blocks whenever it cannot
 * obtain a clean {@code ALLOW} verdict — missing API key, transport error, or
 * an unparseable response. That way untrusted content is never auto-approved
 * just because the safety check was unavailable.</p>
 */
@Service
public class ModerationServiceImpl implements ModerationService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private AiServiceImpl aiService;

    @Autowired
    private AiGuardrailProperties props;

    @Override
    public ModerationResult check(String content) {
        if (!props.isModerationEnabled()) {
            return ModerationResult.allow();
        }
        if (content == null || content.trim().isEmpty()) {
            // Nothing to classify.
            return ModerationResult.allow();
        }
        if (!aiService.isConfigured()) {
            logger.warn("Moderation requested but Claude is not configured; blocking (fail-closed)");
            return ModerationResult.block("moderation unavailable");
        }

        String userMessage = PromptGuardrails.wrapUntrusted(
                PromptGuardrails.truncate(content, props.getMaxInputChars()));
        try {
            String response = aiService.callClaudeApi(PromptGuardrails.MODERATION_SYSTEM_PROMPT, userMessage);
            if (response != null && response.trim().toUpperCase().startsWith("ALLOW")) {
                return ModerationResult.allow();
            }
            String reason = (response == null) ? "no response" : response.trim();
            logger.info("Moderation blocked content: {}", reason);
            return ModerationResult.block(reason);
        } catch (Exception e) {
            logger.warn("Moderation call failed, blocking (fail-closed): {}", e.getMessage());
            return ModerationResult.block("moderation error");
        }
    }
}
