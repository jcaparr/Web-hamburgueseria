package com.hamburguesas.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token bucket per key, held in memory.
 *
 * In memory because the app runs as a single process on a single server: a shared
 * store would add a dependency for no gain today. If it ever runs on more than one
 * instance this has to move to Redis, or each instance will allow the full quota.
 *
 * Buckets refill smoothly rather than resetting on a fixed schedule, which stops the
 * trick of spending a whole window right before it rolls over and again right after.
 */
@Component
@Slf4j
public class RateLimiter {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    private static final class Bucket {
        double tokens;
        long lastRefillNanos;

        Bucket(double tokens, long lastRefillNanos) {
            this.tokens = tokens;
            this.lastRefillNanos = lastRefillNanos;
        }
    }

    /**
     * @return false when the caller has run out of allowance and should be refused.
     */
    public boolean tryAcquire(String key, int capacity, Duration window) {
        long now = System.nanoTime();
        double refillPerNano = (double) capacity / window.toNanos();

        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(capacity, now));

        // Locked per bucket, not globally: two requests for the same key must not both
        // read the same remaining token and each decide they can have it.
        synchronized (bucket) {
            double refilled = (now - bucket.lastRefillNanos) * refillPerNano;
            bucket.tokens = Math.min(capacity, bucket.tokens + refilled);
            bucket.lastRefillNanos = now;

            if (bucket.tokens < 1) {
                return false;
            }
            bucket.tokens -= 1;
            return true;
        }
    }

    /**
     * Drops buckets that are back to full, so the map does not grow one entry per
     * address seen. A full bucket is indistinguishable from one that never existed.
     */
    @Scheduled(fixedDelay = 15, timeUnit = java.util.concurrent.TimeUnit.MINUTES)
    public void evictIdleBuckets() {
        long now = System.nanoTime();
        long idleThresholdNanos = Duration.ofHours(1).toNanos();

        int before = buckets.size();
        buckets.entrySet().removeIf(entry -> {
            Bucket bucket = entry.getValue();
            synchronized (bucket) {
                return now - bucket.lastRefillNanos > idleThresholdNanos;
            }
        });

        int removed = before - buckets.size();
        if (removed > 0) {
            log.debug("Rate limiter: evicted {} idle buckets, {} left", removed, buckets.size());
        }
    }
}
