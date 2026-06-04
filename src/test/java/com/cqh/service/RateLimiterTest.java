package com.cqh.service;

import org.junit.Test;

import static org.junit.Assert.*;

public class RateLimiterTest {

    @Test
    public void zeroLimit_alwaysAllows() {
        RateLimiter rl = new RateLimiter();
        for (int i = 0; i < 100; i++) {
            assertTrue(rl.tryAcquire("k", 0));
        }
    }

    @Test
    public void enforcesLimitWithinWindow() {
        RateLimiter rl = new RateLimiter();
        assertTrue(rl.tryAcquire("k", 2));
        assertTrue(rl.tryAcquire("k", 2));
        assertFalse(rl.tryAcquire("k", 2));
    }

    @Test
    public void separateKeys_independentBuckets() {
        RateLimiter rl = new RateLimiter();
        assertTrue(rl.tryAcquire("a", 1));
        assertFalse(rl.tryAcquire("a", 1));
        assertTrue(rl.tryAcquire("b", 1));
    }
}
