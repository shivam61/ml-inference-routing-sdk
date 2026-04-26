package com.github.placeholder.mlinference.examples;

import com.github.placeholder.mlinference.client.*;
import com.github.placeholder.mlinference.domain.*;
import com.github.placeholder.mlinference.executor.InferenceExecutor;
import com.github.placeholder.mlinference.observability.InMemoryMetricsRecorder;
import com.github.placeholder.mlinference.planner.ExecutionPlanner;
import com.github.placeholder.mlinference.planner.InferencePlan;
import com.github.placeholder.mlinference.registry.ModelRegistry;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class LowLatencyExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== ML Inference Routing SDK: Low Latency & Fallback Example ===");

        ModelRegistry registry = new ModelRegistry();
        // A model with a very tight 10ms timeout
        ModelDefinition slowModel = new ModelDefinition("slow_model", "risk", "v1", BackendType.REMOTE, 10, 10, Set.of(), 
            new ModelDefinition.FallbackConfig(FallbackType.CONSTANT_SCORE, 0.0, Map.of()), null, Map.of());
        registry.register(slowModel);

        InMemoryMetricsRecorder metrics = new InMemoryMetricsRecorder();
        
        // A client that takes 50ms (guaranteed to trigger timeout)
        ModelClient slowClient = new ModelClient() {
            @Override
            public CompletableFuture<List<ModelOutput>> predict(ModelDefinition model, List<ModelInput> inputs, RequestContext ctx) {
                return CompletableFuture.supplyAsync(() -> {
                    try { Thread.sleep(50); } catch (InterruptedException e) {}
                    return inputs.stream().map(i -> new ModelOutput(i.candidateId(), model.modelId(), 1.0, Map.of())).toList();
                });
            }
            @Override public boolean supports(BackendType bt) { return true; }
        };

        try (InferenceExecutor executor = new InferenceExecutor(slowClient, metrics)) {
            ExecutionPlanner planner = new ExecutionPlanner(registry);

            List<Candidate> candidates = List.of(new Candidate("c1", Map.of(), Map.of()));
            
            // Request with 100ms global deadline, but the model has a 10ms timeout
            RequestContext context = new RequestContext("req-fast", "TEST", Instant.now(), Instant.now().plusMillis(100), Map.of(), candidates, Map.of());
            InferencePlan plan = planner.plan(Set.of("slow_model"));

            System.out.println("Executing request...");
            InferenceResult result = executor.execute(plan, context).get(1, TimeUnit.SECONDS);

            System.out.println("Status: " + result.status()); // Should be PARTIAL_SUCCESS
            System.out.println("Fallbacks triggered: " + result.fallbackEvents().size());
            
            ModelOutput output = result.outputsByCandidate().get("c1").get(0);
            System.out.println("Candidate c1 score: " + output.score() + " (Expected 0.0 from fallback)");
            
            metrics.printSummary();
            System.exit(0);
        }
    }
}
