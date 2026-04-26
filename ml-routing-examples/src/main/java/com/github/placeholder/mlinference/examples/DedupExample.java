package com.github.placeholder.mlinference.examples;

import com.github.placeholder.mlinference.client.ModelClient;
import com.github.placeholder.mlinference.client.SimulatedModelClient;
import com.github.placeholder.mlinference.domain.*;
import com.github.placeholder.mlinference.executor.InferenceExecutor;
import com.github.placeholder.mlinference.observability.MetricsRecorder;
import com.github.placeholder.mlinference.planner.ExecutionPlanner;
import com.github.placeholder.mlinference.planner.InferencePlan;
import com.github.placeholder.mlinference.registry.ModelRegistry;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;

public class DedupExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== ML Inference Routing SDK: Deduplication Example ===");

        ModelRegistry registry = new ModelRegistry();
        ModelDefinition model = new ModelDefinition("ranker", "rank", "v1", BackendType.REMOTE, 50, 10, Set.of(), null, null, Map.of());
        registry.register(model);

        InferenceExecutor executor = new InferenceExecutor(
            new SimulatedModelClient(), 
            Executors.newFixedThreadPool(2), 
            Executors.newSingleThreadScheduledExecutor(), 
            MetricsRecorder.NOOP
        );
        ExecutionPlanner planner = new ExecutionPlanner(registry);

        // Create candidates with DUPLICATE features
        Map<String, Object> commonFeatures = Map.of("f1", 0.5, "f2", 1.0);
        List<Candidate> candidates = List.of(
            new Candidate("c1", commonFeatures, Map.of()),
            new Candidate("c2", commonFeatures, Map.of()), // Duplicate!
            new Candidate("c3", Map.of("f1", 0.1), Map.of())
        );

        RequestContext context = new RequestContext("req-dedup", "TEST", Instant.now(), Instant.now().plusSeconds(1), Map.of(), candidates, Map.of());
        InferencePlan plan = planner.plan(Set.of("ranker"));

        executor.execute(plan, context).thenAccept(result -> {
            System.out.println("Total Outputs: " + result.outputsByCandidate().size());
            long dedupHits = result.executionTrace().events().stream()
                .filter(e -> "DEDUP_HIT".equals(e.eventType()))
                .count();
            System.out.println("Deduplication Hits: " + dedupHits);
            System.out.println("Expectation: c2 should be served from c1's result cache.");
            System.exit(0);
        });
    }
}
