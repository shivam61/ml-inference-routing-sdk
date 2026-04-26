package io.github.shivam61.mlinference.client;

import io.github.shivam61.mlinference.domain.BackendType;
import io.github.shivam61.mlinference.domain.ModelDefinition;
import io.github.shivam61.mlinference.domain.ModelInput;
import io.github.shivam61.mlinference.domain.ModelOutput;
import io.github.shivam61.mlinference.domain.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Model client for NVIDIA Triton Inference Server.
 * Simulates a gRPC-style call.
 */
public class TritonModelClient implements ModelClient {
    private static final Logger logger = LoggerFactory.getLogger(TritonModelClient.class);

    @Override
    public CompletableFuture<List<ModelOutput>> predict(
            ModelDefinition model,
            List<ModelInput> inputs,
            RequestContext ctx) {
        
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Executing Triton gRPC call to {} for model {}", model.endpoint(), model.modelId());
            
            // Simulate processing time
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }

            return inputs.stream()
                    .map(input -> new ModelOutput(
                            input.candidateId(),
                            model.modelId(),
                            0.95, // Simulated score
                            java.util.Map.of("backend", "triton")
                    ))
                    .collect(Collectors.toList());
        });
    }

    @Override
    public boolean supports(BackendType backendType) {
        return backendType == BackendType.TRITON;
    }
}
