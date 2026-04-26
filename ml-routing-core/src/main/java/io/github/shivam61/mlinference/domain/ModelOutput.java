package io.github.shivam61.mlinference.domain;

import java.util.Map;

public record ModelOutput(
    String candidateId,
    String modelId,
    double score,
    Map<String, Object> metadata
) {}
