package io.github.shivam61.mlinference.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Lightweight internal circuit breaker.
 */
public class CircuitBreaker {
    private static final Logger logger = LoggerFactory.getLogger(CircuitBreaker.class);

    public enum State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    private final String modelId;
    private final int failureThreshold;
    private final Duration resetTimeout;
    
    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicReference<Instant> lastTripTime = new AtomicReference<>();

    public CircuitBreaker(String modelId, int failureThreshold, Duration resetTimeout) {
        this.modelId = modelId;
        this.failureThreshold = failureThreshold;
        this.resetTimeout = resetTimeout;
    }

    public boolean allowRequest() {
        State currentState = state.get();
        if (currentState == State.OPEN) {
            if (Instant.now().isAfter(lastTripTime.get().plus(resetTimeout))) {
                if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                    logger.info("Circuit breaker for model {} entering HALF_OPEN state", modelId);
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    public void recordSuccess() {
        if (state.get() == State.HALF_OPEN) {
            state.set(State.CLOSED);
            failureCount.set(0);
            logger.info("Circuit breaker for model {} recovered to CLOSED state", modelId);
        } else {
            failureCount.set(0);
        }
    }

    public void recordFailure() {
        int failures = failureCount.incrementAndGet();
        if (failures >= failureThreshold) {
            if (state.compareAndSet(State.CLOSED, State.OPEN) || state.compareAndSet(State.HALF_OPEN, State.OPEN)) {
                lastTripTime.set(Instant.now());
                logger.warn("Circuit breaker for model {} TRIP to OPEN state due to {} failures", modelId, failures);
            }
        }
    }

    public State getState() {
        return state.get();
    }
}
