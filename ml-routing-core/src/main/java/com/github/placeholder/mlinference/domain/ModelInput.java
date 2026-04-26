package com.github.placeholder.mlinference.domain;

import java.util.Map;

public record ModelInput(
    String candidateId,
    Map<String, Object> features
) {}
