package com.github.placeholder.mlinference.planner;

import com.github.placeholder.mlinference.domain.ModelDefinition;
import java.util.List;
import java.util.Set;

/**
 * Represents a structured inference execution plan.
 * Contains the models to execute, organized into stages based on dependencies.
 */
public record InferencePlan(
    List<ExecutionStage> stages,
    Set<String> selectedModelIds
) {
    /**
     * A stage represents a group of models that can be executed in parallel.
     */
    public record ExecutionStage(
        int stageNumber,
        List<ModelDefinition> models
    ) {}
}
