package com.github.placeholder.mlinference.domain;

import java.util.Map;

public record Candidate(
    String candidateId,
    Map<String, Object> features,
    Map<String, Object> attributes
) {}
