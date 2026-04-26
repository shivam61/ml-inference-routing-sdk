package io.github.shivam61.mlinference.client;

import io.github.shivam61.mlinference.domain.*;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Simulates a remote model serving endpoint with configurable latency.
 */
public class SimulatedModelClient implements ModelClient {
    private final Random random = new Random();

    @Override
    public CompletableFuture<List<ModelOutput>> predict(ModelDefinition model, List<ModelInput> inputs, RequestContext ctx) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Simulate network latency
                long latency = model.timeoutMs() > 0 ? (long) (model.timeoutMs() * 0.8) : 5;
                Thread.sleep(latency);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            return inputs.stream()
                .map(input -> new ModelOutput(
                    input.candidateId(),
                    model.modelId(),
                    random.nextDouble(),
                    java.util.Map.of("version", model.version())
                ))
                .toList();
        }, CompletableFuture.delayedExecutor(0, TimeUnit.MILLISECONDS)); // Use default forkjoin for simulation
    }

    @Override
    public boolean supports(BackendType backendType) {
        return backendType == BackendType.REMOTE;
    }
}
