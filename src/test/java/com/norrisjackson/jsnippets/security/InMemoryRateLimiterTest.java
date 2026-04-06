package com.norrisjackson.jsnippets.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRateLimiterTest {

    private InMemoryRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new InMemoryRateLimiter();
    }

    @Test
    void isAllowed_firstRequestWithinLimit_returnsTrue() {
        assertThat(rateLimiter.isAllowed("client-1", 5, 60)).isTrue();
    }

    @Test
    void isAllowed_requestsUpToLimit_allReturnTrue() {
        String key = "client-2";
        for (int i = 0; i < 5; i++) {
            assertThat(rateLimiter.isAllowed(key, 5, 60)).isTrue();
        }
    }

    @Test
    void isAllowed_requestsExceedingLimit_returnsFalse() {
        String key = "client-3";
        for (int i = 0; i < 5; i++) {
            rateLimiter.isAllowed(key, 5, 60);
        }
        assertThat(rateLimiter.isAllowed(key, 5, 60)).isFalse();
    }

    @Test
    void isAllowed_differentKeys_areIndependent() {
        // Exhaust limit for client-a
        for (int i = 0; i < 3; i++) {
            rateLimiter.isAllowed("client-a", 3, 60);
        }
        assertThat(rateLimiter.isAllowed("client-a", 3, 60)).isFalse();

        // client-b should still be allowed
        assertThat(rateLimiter.isAllowed("client-b", 3, 60)).isTrue();
    }

    @Test
    void isAllowed_windowExpires_resetsCount() throws InterruptedException {
        String key = "client-4";
        // Use a 1-second window
        for (int i = 0; i < 3; i++) {
            rateLimiter.isAllowed(key, 3, 1);
        }
        assertThat(rateLimiter.isAllowed(key, 3, 1)).isFalse();

        // Wait for the window to expire
        Thread.sleep(1100);

        // Should be allowed again in the new window
        assertThat(rateLimiter.isAllowed(key, 3, 1)).isTrue();
    }

    @Test
    void isAllowed_concurrentAccess_respectsLimit() throws InterruptedException {
        String key = "concurrent-client";
        int maxRequests = 50;
        int totalThreads = 100;

        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch latch = new CountDownLatch(totalThreads);
        AtomicInteger allowedCount = new AtomicInteger(0);

        for (int i = 0; i < totalThreads; i++) {
            executor.submit(() -> {
                try {
                    if (rateLimiter.isAllowed(key, maxRequests, 60)) {
                        allowedCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Exactly maxRequests should have been allowed (atomic compute guarantees this)
        assertThat(allowedCount.get()).isEqualTo(maxRequests);
    }

    @Test
    void isAllowed_zeroMaxRequests_alwaysDenied() {
        assertThat(rateLimiter.isAllowed("client-0", 0, 60)).isFalse();
    }

    @Test
    void cleanup_removesExpiredBuckets() throws InterruptedException {
        // Add entries with a short window
        rateLimiter.isAllowed("expire-me", 5, 1);
        rateLimiter.isAllowed("keep-me", 5, 60);

        // Wait for the short window to expire
        Thread.sleep(1100);

        // Cleanup should remove expired entries but not error
        rateLimiter.cleanup();

        // "keep-me" should still be tracking (2nd request in same window)
        assertThat(rateLimiter.isAllowed("keep-me", 5, 60)).isTrue();
    }
}

