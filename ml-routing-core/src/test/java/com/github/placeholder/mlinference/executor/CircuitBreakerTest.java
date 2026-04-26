package com.github.placeholder.mlinference.executor;

import org.junit.jupiter.api.Test;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class CircuitBreakerTest {

    @Test
    void shouldOpenBreakerAfterFailures() {
        CircuitBreaker cb = new CircuitBreaker("m1", 3, Duration.ofSeconds(1));
        
        assertThat(cb.allowRequest()).isTrue();
        
        cb.recordFailure();
        cb.recordFailure();
        cb.recordFailure();
        
        assertThat(cb.allowRequest()).isFalse();
    }

    @Test
    void shouldCloseBreakerAfterResetTimeout() throws InterruptedException {
        CircuitBreaker cb = new CircuitBreaker("m1", 1, Duration.ofMillis(100));
        
        cb.recordFailure();
        assertThat(cb.allowRequest()).isFalse();
        
        Thread.sleep(150);
        
        assertThat(cb.allowRequest()).isTrue();
    }

    @Test
    void shouldResetCountOnSuccess() {
        CircuitBreaker cb = new CircuitBreaker("m1", 2, Duration.ofSeconds(1));
        
        cb.recordFailure();
        cb.recordSuccess();
        cb.recordFailure();
        
        assertThat(cb.allowRequest()).isTrue();
    }
}
