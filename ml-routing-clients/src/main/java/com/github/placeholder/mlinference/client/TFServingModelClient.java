package com.github.placeholder.mlinference.client;

import com.github.placeholder.mlinference.domain.BackendType;
import com.github.placeholder.mlinference.domain.ModelDefinition;
import com.github.placeholder.mlinference.domain.ModelInput;
import com.github.placeholder.mlinference.domain.ModelOutput;
import com.github.placeholder.mlinference.domain.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Model client for TensorFlow Serving.
 * Simulates a REST/gRPC-style call.
 */
public class TFServingModelClient implements ModelClient {
    private static final Logger logger = LoggerFactory.getLogger(TFServingModelClient.class);

    @Override
    public CompletableFuture<List<ModelOutput>> predict(
            ModelDefinition model,
            List<ModelInput> inputs,
            RequestContext ctx) {
        
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Executing TF Serving call to {} for model {}", model.endpoint(), model.modelId());
            
            // Simulate processing time
            try {
                Thread.sleep(15);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }

            return inputs.stream()
                    .map(input -> new ModelOutput(
                            input.candidateId(),
                            0.88, // Simulated score
                            java.util.Map.of("backend", "tf_serving")
                    ))
                    .collect(Collectors.toList());
        });
    }

    @Override
    public boolean supports(BackendType backendType) {
        return backendType == BackendType.TF_SERVING;
    }
}
