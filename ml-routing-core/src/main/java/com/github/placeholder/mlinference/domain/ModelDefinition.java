package com.github.placeholder.mlinference.domain;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record ModelDefinition(
    String modelId,
    String modelFamily,
    String version,
    BackendType backendType,
    long timeoutMs,
    int maxBatchSize,
    Set<String> dependencies,
    FallbackConfig fallbackStrategy,
    LazyPredicateConfig lazyPredicate,
    Map<String, String> tags
) {
    public record FallbackConfig(
        FallbackType type,
        Double value,
        Map<String, Object> metadata
    ) {}

    public record LazyPredicateConfig(
        String type,
        String upstreamModel,
        Map<String, Object> params
    ) {}
}
