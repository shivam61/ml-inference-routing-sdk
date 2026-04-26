package io.github.shivam61.mlinference.client;

import io.github.shivam61.mlinference.domain.ModelDefinition;
import io.github.shivam61.mlinference.domain.ModelInput;
import io.github.shivam61.mlinference.domain.ModelOutput;
import io.github.shivam61.mlinference.domain.RequestContext;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for executing model predictions.
 */
public interface ModelClient {
    /**
     * Executes prediction for a batch of inputs.
     * 
     * @param model The model definition.
     * @param inputs The list of inputs for candidates.
     * @param ctx The request context for deadline and tracing.
     * @return A future containing the list of model outputs.
     */
    CompletableFuture<List<ModelOutput>> predict(
        ModelDefinition model, 
        List<ModelInput> inputs, 
        RequestContext ctx
    );

    /**
     * Returns whether this client supports the given backend type.
     */
    boolean supports(io.github.shivam61.mlinference.domain.BackendType backendType);
}
