package com.github.placeholder.mlinference.domain;

import java.time.Instant;
import java.util.List;

public record ExecutionTrace(
    List<ExecutionEvent> events
) {}
