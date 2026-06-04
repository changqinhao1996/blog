package com.cqh.service;

/**
 * Content-safety classifier for <em>untrusted</em> text (e.g. guest posts,
 * imported articles, comment bodies) before it is processed or published.
 *
 * <p>Infrastructure for the planned guest-posting / import / comment-summary
 * features — it is not yet wired into any publish flow. Intended call site:
 * the service/controller that accepts untrusted content, gating publication on
 * {@link ModerationResult#isAllowed()} with <strong>fail-closed</strong>
 * semantics (block when the verdict cannot be obtained).</p>
 */
public interface ModerationService {

    ModerationResult check(String content);

    /** Outcome of a moderation check. */
    final class ModerationResult {

        private final boolean allowed;
        private final String reason;

        public ModerationResult(boolean allowed, String reason) {
            this.allowed = allowed;
            this.reason = reason;
        }

        public boolean isAllowed() {
            return allowed;
        }

        /** Human-readable reason when blocked; null when allowed. */
        public String getReason() {
            return reason;
        }

        public static ModerationResult allow() {
            return new ModerationResult(true, null);
        }

        public static ModerationResult block(String reason) {
            return new ModerationResult(false, reason);
        }
    }
}
