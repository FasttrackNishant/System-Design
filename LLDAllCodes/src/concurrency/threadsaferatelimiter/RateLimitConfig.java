package concurrency.threadsaferatelimiter;

import java.time.Duration;

public final class RateLimitConfig {
    private final double tokensPerSecond;
    private final long capacity;
    private final Duration cleanupInterval;
    private final Duration inactivityTimeout;

    public RateLimitConfig(double tokensPerSecond, long capacity) {
        this(tokensPerSecond, capacity,
             Duration.ofMinutes(1), Duration.ofMinutes(5));
    }

    public RateLimitConfig(double tokensPerSecond, long capacity,
                          Duration cleanupInterval, Duration inactivityTimeout) {
        if (tokensPerSecond <= 0) {
            throw new IllegalArgumentException("tokensPerSecond must be positive");
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.tokensPerSecond = tokensPerSecond;
        this.capacity = capacity;
        this.cleanupInterval = cleanupInterval;
        this.inactivityTimeout = inactivityTimeout;
    }

    public double getTokensPerSecond() { return tokensPerSecond; }
    public long getCapacity() { return capacity; }
    public Duration getCleanupInterval() { return cleanupInterval; }
    public Duration getInactivityTimeout() { return inactivityTimeout; }
}









import java.util.concurrent.atomic.AtomicLong;

public class TokenBucket {
    // Precision multiplier to store fractional tokens as long
    private static final long PRECISION = 1_000_000L;

    private final AtomicLong tokens;
    private final AtomicLong lastRefillTimeNanos;
    private final AtomicLong lastAccessTimeNanos;
    private final long capacityScaled;
    private final double tokensPerSecond;

    public TokenBucket(long capacity, double tokensPerSecond) {
        this.capacityScaled = capacity * PRECISION;
        this.tokens = new AtomicLong(capacityScaled);  // Start full
        this.tokensPerSecond = tokensPerSecond;
        long now = System.nanoTime();
        this.lastRefillTimeNanos = new AtomicLong(now);
        this.lastAccessTimeNanos = new AtomicLong(now);
    }

    /**
     * Attempts to acquire one token.
     * Uses CAS loop for lock-free operation.
     */
    public boolean tryAcquire() {
        return tryAcquire(1);
    }

    /**
     * Attempts to acquire multiple tokens.
     * Atomically refills and consumes in a single operation using CAS.
     */
    public boolean tryAcquire(int permits) {
        long permitsScaled = permits * PRECISION;

        // CAS retry loop: keeps trying until we either succeed or determine
        // there aren't enough tokens. Under low contention, succeeds first try.
        while (true) {
            long now = System.nanoTime();  // Monotonic clock (never goes backward)
            long currentTokens = tokens.get();       // Step 1: Read current state
            long lastRefill = lastRefillTimeNanos.get();

            // Step 2: Calculate what the token count SHOULD be after refill
            double elapsedSeconds = (now - lastRefill) / 1_000_000_000.0;
            long tokensToAdd = (long) (elapsedSeconds * tokensPerSecond * PRECISION);
            long newTokens = Math.min(capacityScaled, currentTokens + tokensToAdd);

            // Step 3: Check if enough tokens (may exit early without CAS)
            if (newTokens < permitsScaled) {
                // Update last access even on rejection (for cleanup tracking)
                lastAccessTimeNanos.set(now);
                return false;
            }

            // Step 4: Calculate post-consumption value
            long afterConsume = newTokens - permitsScaled;

            // Step 5: CAS - atomically update IF no one else changed it
            // This is the critical atomic operation that prevents races
            if (tokens.compareAndSet(currentTokens, afterConsume)) {
                // Success! Update timestamps (best effort, slight drift is acceptable)
                lastRefillTimeNanos.set(now);
                lastAccessTimeNanos.set(now);
                return true;
            }
            // CAS failed - another thread modified tokens between our read and CAS.
            // Loop back and retry with fresh values.
        }
    }

    /**
     * Returns current available tokens after refilling.
     * For monitoring and debugging.
     */
    public double getAvailableTokens() {
        long now = System.nanoTime();
        long currentTokens = tokens.get();
        long lastRefill = lastRefillTimeNanos.get();

        double elapsedSeconds = (now - lastRefill) / 1_000_000_000.0;
        long tokensToAdd = (long) (elapsedSeconds * tokensPerSecond * PRECISION);
        long available = Math.min(capacityScaled, currentTokens + tokensToAdd);

        return available / (double) PRECISION;
    }

    /**
     * Returns last access time in nanoseconds.
     * Used by cleanup to identify stale buckets.
     */
    public long getLastAccessTimeNanos() {
        return lastAccessTimeNanos.get();
    }

    /**
     * Returns capacity for informational purposes.
     */
    public long getCapacity() {
        return capacityScaled / PRECISION;
    }

    /**
     * Returns refill rate for informational purposes.
     */
    public double getTokensPerSecond() {
        return tokensPerSecond;
    }
}





















import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class RateLimiter {
    private final ConcurrentHashMap<String, TokenBucket> buckets;
    private final RateLimitConfig config;
    private final ScheduledExecutorService cleanupScheduler;
    private final AtomicBoolean running;

    public RateLimiter(RateLimitConfig config) {
        this.config = config;
        this.buckets = new ConcurrentHashMap<>();
        this.running = new AtomicBoolean(true);

        // Start cleanup scheduler
        // Daemon thread: won't prevent JVM shutdown if main threads are done
        this.cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rate-limiter-cleanup");
            t.setDaemon(true);  // Important: daemon threads don't block shutdown
            return t;
        });

        // Schedule periodic cleanup to remove stale buckets (memory management)
        cleanupScheduler.scheduleAtFixedRate(
            this::cleanupStaleBuckets,
            config.getCleanupInterval().toMillis(),
            config.getCleanupInterval().toMillis(),
            TimeUnit.MILLISECONDS
        );
    }

    /**
     * Convenience constructor with default config.
     */
    public RateLimiter(double tokensPerSecond, long capacity) {
        this(new RateLimitConfig(tokensPerSecond, capacity));
    }

    /**
     * Attempts to acquire a permit for the given client.
     * Returns true if allowed, false if rate limited.
     */
    public boolean tryAcquire(String clientId) {
        if (!running.get()) {
            throw new IllegalStateException("RateLimiter is shut down");
        }

        // computeIfAbsent is atomic - prevents double bucket creation
        TokenBucket bucket = buckets.computeIfAbsent(clientId,
            k -> new TokenBucket(config.getCapacity(), config.getTokensPerSecond())
        );

        return bucket.tryAcquire();
    }

    /**
     * Attempts to acquire multiple permits for the given client.
     */
    public boolean tryAcquire(String clientId, int permits) {
        if (!running.get()) {
            throw new IllegalStateException("RateLimiter is shut down");
        }
        if (permits <= 0) {
            throw new IllegalArgumentException("permits must be positive");
        }

        TokenBucket bucket = buckets.computeIfAbsent(clientId,
            k -> new TokenBucket(config.getCapacity(), config.getTokensPerSecond())
        );

        return bucket.tryAcquire(permits);
    }

    /**
     * Returns information about a client's current rate limit state.
     */
    public RateLimitInfo getClientInfo(String clientId) {
        TokenBucket bucket = buckets.get(clientId);
        if (bucket == null) {
            // No bucket = full capacity available
            return new RateLimitInfo(
                config.getCapacity(),
                config.getCapacity(),
                config.getTokensPerSecond()
            );
        }

        return new RateLimitInfo(
            bucket.getAvailableTokens(),
            bucket.getCapacity(),
            bucket.getTokensPerSecond()
        );
    }

    /**
     * Removes buckets that haven't been accessed recently.
     * Called periodically by the cleanup scheduler.
     *
     * Why cleanup? Without it, buckets for one-time clients would accumulate
     * forever, causing memory leaks in long-running services.
     */
    private void cleanupStaleBuckets() {
        long now = System.nanoTime();
        long timeoutNanos = config.getInactivityTimeout().toNanos();

        // Thread-safe maps allow safe iteration during concurrent modification.
        // removeIf is atomic per-entry: either the entry is removed or it's not.
        buckets.entrySet().removeIf(entry -> {
            TokenBucket bucket = entry.getValue();
            long lastAccess = bucket.getLastAccessTimeNanos();
            // Remove bucket if it hasn't been accessed within the timeout period
            return (now - lastAccess) > timeoutNanos;
        });
    }

    /**
     * Returns the number of active client buckets.
     * For monitoring purposes.
     */
    public int getActiveBucketCount() {
        return buckets.size();
    }

    /**
     * Shuts down the rate limiter.
     * Stops cleanup scheduler and clears buckets.
     */
    public void shutdown() {
        if (running.compareAndSet(true, false)) {
            cleanupScheduler.shutdown();
            try {
                if (!cleanupScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    cleanupScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                cleanupScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            buckets.clear();
        }
    }
}

/**
 * Information about a client's rate limit state.
 */
record RateLimitInfo(double remainingTokens, long capacity, double tokensPerSecond) {
    public double getResetTimeSeconds() {
        if (remainingTokens >= capacity) {
            return 0;
        }
        return (capacity - remainingTokens) / tokensPerSecond;
    }
}







































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































