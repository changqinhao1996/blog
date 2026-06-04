package com.cqh.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minimal in-memory fixed-window rate limiter, keyed by an arbitrary string.
 * No external dependency — adequate for a single-instance deployment. A
 * multi-instance production setup would back this with Redis instead.
 */
@Component
public class RateLimiter {

    private static final long WINDOW_MS = 60_000L;

    private static final class Window {
        long start;
        int count;
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<String, Window>();

    /**
     * @param key          the bucket key (e.g. the feature name)
     * @param maxPerMinute max permits per rolling minute; {@code <= 0} disables limiting
     * @return true if a permit was granted, false if the window is exhausted
     */
    public synchronized boolean tryAcquire(String key, int maxPerMinute) {
        if (maxPerMinute <= 0) {
            return true;
        }
        long now = System.currentTimeMillis();
        Window w = windows.get(key);
        if (w == null || now - w.start >= WINDOW_MS) {
            w = new Window();
            w.start = now;
            windows.put(key, w);
        }
        if (w.count >= maxPerMinute) {
            return false;
        }
        w.count++;
        return true;
    }
}
