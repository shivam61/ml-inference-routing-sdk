package com.github.placeholder.mlinference.domain;

import java.time.Instant;
import java.util.Map;

public record ExecutionEvent(
    Instant timestamp,
    String eventType,
    String modelId,
    Map<String, Object> metadata
) {}
