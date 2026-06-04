package com.cqh.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Tunable settings for the AI guardrail layer (see {@code claude.guardrail.*}
 * in application.yml). Guardrails are <strong>on by default</strong>; set
 * {@code claude.guardrail.enabled: false} to fall back to the raw,
 * un-guarded behaviour.
 */
@Component
@ConfigurationProperties(prefix = "claude.guardrail")
public class AiGuardrailProperties {

    /** Master switch. When false the guardrail layer is a transparent pass-through. */
    private boolean enabled = true;

    /** Max characters of untrusted content sent to the model. 0 = unlimited. */
    private int maxInputChars = 20000;

    /** Max characters kept from a generated summary/description. 0 = unlimited. */
    private int maxOutputChars = 1000;

    /** Max AI calls per feature per rolling minute. 0 = unlimited. */
    private int rateLimitPerMinute = 30;

    /** Whether the moderation classifier is active (fail-closed when enabled). */
    private boolean moderationEnabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxInputChars() {
        return maxInputChars;
    }

    public void setMaxInputChars(int maxInputChars) {
        this.maxInputChars = maxInputChars;
    }

    public int getMaxOutputChars() {
        return maxOutputChars;
    }

    public void setMaxOutputChars(int maxOutputChars) {
        this.maxOutputChars = maxOutputChars;
    }

    public int getRateLimitPerMinute() {
        return rateLimitPerMinute;
    }

    public void setRateLimitPerMinute(int rateLimitPerMinute) {
        this.rateLimitPerMinute = rateLimitPerMinute;
    }

    public boolean isModerationEnabled() {
        return moderationEnabled;
    }

    public void setModerationEnabled(boolean moderationEnabled) {
        this.moderationEnabled = moderationEnabled;
    }
}
