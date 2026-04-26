package io.github.shivam61.mlinference.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record InferenceResult(
    String requestId,
    ExecutionStatus status,
    Map<String, List<ModelOutput>> outputsByModel,
    Map<String, List<ModelOutput>> outputsByCandidate,
    List<FallbackEvent> fallbackEvents,
    ExecutionTrace executionTrace,
    ExecutionStats stats,
    long totalLatencyMs
) {
    public record FallbackEvent(
        String modelId,
        String candidateId,
        FallbackType type,
        String reason
    ) {}
}
