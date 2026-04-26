package io.github.shivam61.mlinference.domain;

import java.util.Map;

public record ModelInput(
    String candidateId,
    Map<String, Object> features
) {}
