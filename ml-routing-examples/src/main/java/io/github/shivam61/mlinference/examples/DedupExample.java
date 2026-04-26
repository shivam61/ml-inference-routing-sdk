package io.github.shivam61.mlinference.examples;

import io.github.shivam61.mlinference.client.ModelClient;
import io.github.shivam61.mlinference.client.SimulatedModelClient;
import io.github.shivam61.mlinference.domain.*;
import io.github.shivam61.mlinference.executor.InferenceExecutor;
import io.github.shivam61.mlinference.observability.MetricsRecorder;
import io.github.shivam61.mlinference.planner.ExecutionPlanner;
import io.github.shivam61.mlinference.planner.InferencePlan;
import io.github.shivam61.mlinference.registry.ModelRegistry;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;

public class DedupExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== ML Inference Routing SDK: Deduplication Example ===");

        ModelRegistry registry = new ModelRegistry();
        ModelDefinition model = new ModelDefinition("ranker", "rank", "v1", "", BackendType.REMOTE, 50, 10, Set.of(), null, null, Map.of());
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
            System.out.println(result.stats().explain());
            System.out.println("Expectation: c2 should be served from c1's result cache.");
            System.exit(0);
        });
    }
}
