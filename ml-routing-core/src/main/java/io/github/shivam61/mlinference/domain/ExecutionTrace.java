package io.github.shivam61.mlinference.domain;

import java.time.Instant;
import java.util.List;

public record ExecutionTrace(
    List<ExecutionEvent> events
) {}
