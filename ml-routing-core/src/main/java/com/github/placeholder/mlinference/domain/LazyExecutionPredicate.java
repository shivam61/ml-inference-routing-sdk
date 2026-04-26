package com.github.placeholder.mlinference.domain;

import java.util.List;
import java.util.Map;

/**
 * Interface for defining custom pruning logic between DAG stages.
 */
@FunctionalInterface
public interface LazyExecutionPredicate {
    /**
     * Determines if a specific candidate should be sent to the model for inference.
     *
     * @param model Current model definition.
     * @param candidate The candidate being evaluated.
     * @param existingOutputs Outputs from models in previous stages.
     * @return true if the model should be executed, false if the candidate should be pruned.
     */
    boolean shouldExecute(ModelDefinition model, Candidate candidate, Map<String, List<ModelOutput>> existingOutputs);
}
