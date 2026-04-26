package io.github.shivam61.mlinference.domain;

import java.util.Map;

public record Candidate(
    String candidateId,
    Map<String, Object> features,
    Map<String, Object> attributes
) {}
