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
        List<com.github.placeholder.mlinference.domain.ModelDefinition> models
    ) {}

    public String explain() {
        StringBuilder sb = new StringBuilder();
        sb.append("Inference Execution Plan\n");
        sb.append("========================\n");
        sb.append(String.format("Critical Path Length: %d stages\n", stages.size()));
        sb.append(String.format("Total Models: %d\n", stages.stream().mapToLong(s -> s.models().size()).sum()));
        
        Map<com.github.placeholder.mlinference.domain.BackendType, Long> backendCounts = stages.stream()
            .flatMap(s -> s.models().stream())
            .collect(java.util.stream.Collectors.groupingBy(m -> m.backendType(), java.util.stream.Collectors.counting()));
        
        sb.append("Model Count by Backend:\n");
        backendCounts.forEach((k, v) -> sb.append(String.format("  - %s: %d\n", k, v)));

        sb.append("\nExecution Stages:\n");
        for (ExecutionStage stage : stages) {
            sb.append(String.format("  Stage %d:\n", stage.stageNumber()));
            for (var model : stage.models()) {
                sb.append(String.format("    - %s (%s, timeout: %dms)\n", 
                    model.modelId(), model.backendType(), model.timeoutMs()));
            }
        }
        return sb.toString();
    }
}
