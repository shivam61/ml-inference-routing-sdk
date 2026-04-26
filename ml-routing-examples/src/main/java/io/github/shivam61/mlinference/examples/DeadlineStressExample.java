package io.github.shivam61.mlinference.examples;

import io.github.shivam61.mlinference.client.*;
import io.github.shivam61.mlinference.domain.*;
import io.github.shivam61.mlinference.executor.InferenceExecutor;
import io.github.shivam61.mlinference.observability.InMemoryMetricsRecorder;
import io.github.shivam61.mlinference.planner.ExecutionPlanner;
import io.github.shivam61.mlinference.planner.InferencePlan;
import io.github.shivam61.mlinference.registry.ModelRegistry;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Demonstrates how the SDK handles tight deadlines and slow models.
 * It simulates a heavy model that is too slow for the budget, triggering fallbacks.
 */
public class DeadlineStressExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== ML Inference Routing SDK: Deadline Stress Example ===");

        // 1. Setup Registry
        ModelRegistry registry = new ModelRegistry();
        
        // Fast model (local)
        ModelDefinition lightModel = new ModelDefinition("light_ranker", "rank", "v1", "local", BackendType.IN_MEMORY, 5, 100, Set.of(), null, null, Map.of());
        
        // Heavy model with 30ms timeout and fallback
        ModelDefinition heavyModel = new ModelDefinition("deep_ranker", "rank", "v1", "remote", BackendType.REMOTE, 30, 32, Set.of("light_ranker"), 
            new ModelDefinition.FallbackConfig(FallbackType.CONSTANT_SCORE, 0.5, Map.of()), null, Map.of());
        
        // Aggregator
        ModelDefinition finalModel = new ModelDefinition("final_aggregator", "final", "v1", "local", BackendType.IN_MEMORY, 10, 100, Set.of("deep_ranker"), null, null, Map.of());

        registry.register(lightModel);
        registry.register(heavyModel);
        registry.register(finalModel);

        // 2. Setup Clients
        ModelClient client = new ModelClient() {
            @Override
            public CompletableFuture<List<ModelOutput>> predict(ModelDefinition model, List<ModelInput> inputs, RequestContext ctx) {
                return CompletableFuture.supplyAsync(() -> {
                    try {
                        if ("deep_ranker".equals(model.modelId())) {
                            // Simulate a SLOW model that will exceed the 30ms timeout
                            Thread.sleep(100); 
                        } else {
                            Thread.sleep(2);
                        }
                    } catch (InterruptedException e) {}
                    return inputs.stream().map(i -> new ModelOutput(i.candidateId(), model.modelId(), 0.9, Map.of())).toList();
                });
            }
            @Override public boolean supports(BackendType bt) { return true; }
        };

        InMemoryMetricsRecorder metrics = new InMemoryMetricsRecorder();
        
        try (InferenceExecutor executor = new InferenceExecutor(client, metrics)) {
            ExecutionPlanner planner = new ExecutionPlanner(registry);

            List<Candidate> candidates = List.of(new Candidate("c1", Map.of(), Map.of()));
            
            // Total budget for the whole request: 50ms
            RequestContext context = new RequestContext("req-stress", "SEARCH", Instant.now(), 
                Instant.now().plusMillis(50), Map.of(), candidates, Map.of());
            
            InferencePlan plan = planner.plan(Set.of("final_aggregator"));
            System.out.println(plan.explain());

            System.out.println("Executing request with tight 50ms budget...");
            InferenceResult result = executor.execute(plan, context).get(2, TimeUnit.SECONDS);

            System.out.println("\n--- Results ---");
            System.out.println("Overall Status: " + result.status()); // Should be PARTIAL_SUCCESS
            System.out.println(result.stats().explain());
            
            System.out.println("Final Output for c1: " + result.outputsByCandidate().get("c1").stream()
                .filter(o -> "final_aggregator".equals(o.modelId())).findFirst().orElseThrow().score());
            
            System.out.println("\nResiliency Event Log:");
            result.fallbackEvents().forEach(f -> 
                System.out.println("  [FALLBACK] Model " + f.modelId() + " triggered " + f.type() + " due to " + f.reason()));
            
            metrics.printSummary();
            System.exit(0);
        }
    }
}
