package io.github.shivam61.mlinference.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record RequestContext(
    String requestId,
    String requestType,
    Instant startTime,
    Instant deadline,
    Map<String, Object> attributes,
    List<Candidate> candidates,
    Map<String, Object> executionFlags
) {
    public long getRemainingTimeMs() {
        if (deadline == null) return Long.MAX_VALUE;
        return Math.max(0, deadline.toEpochMilli() - Instant.now().toEpochMilli());
    }

    public boolean isDeadlineExceeded() {
        return getRemainingTimeMs() <= 0;
    }
}
