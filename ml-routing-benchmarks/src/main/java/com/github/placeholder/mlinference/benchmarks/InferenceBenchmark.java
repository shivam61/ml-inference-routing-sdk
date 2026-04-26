package com.github.placeholder.mlinference.benchmarks;

import com.github.placeholder.mlinference.client.*;
import com.github.placeholder.mlinference.domain.*;
import com.github.placeholder.mlinference.executor.InferenceExecutor;
import com.github.placeholder.mlinference.observability.MetricsRecorder;
import com.github.placeholder.mlinference.planner.ExecutionPlanner;
import com.github.placeholder.mlinference.planner.InferencePlan;
import com.github.placeholder.mlinference.registry.ModelRegistry;
import com.github.placeholder.mlinference.vector.activation.ActivationFunction;
import com.github.placeholder.mlinference.vector.engine.FeedForwardNetwork;
import com.github.placeholder.mlinference.vector.engine.LocalVectorizedModelClient;
import com.github.placeholder.mlinference.vector.layer.VectorizedDenseLayer;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

public class InferenceBenchmark {
    public static void main(String[] args) throws Exception {
        System.out.println("=== ML Inference Routing SDK: Performance Benchmarks ===");
        
        int candidateCount = 100;
        int iterations = 5;

        // Setup Registry
        ModelRegistry registry = new ModelRegistry();
        ModelDefinition lightModel = new ModelDefinition("light", "rank", "v1", BackendType.LOCAL_VECTOR, 5, 100, Set.of(), null, null, Map.of());
        ModelDefinition heavyModel = new ModelDefinition("heavy", "rank", "v1", BackendType.REMOTE, 20, 32, Set.of("light"), null, null, Map.of());
        registry.register(lightModel);
        registry.register(heavyModel);

        // Setup Clients
        LocalVectorizedModelClient vectorClient = new LocalVectorizedModelClient();
        vectorClient.registerModel("light", new FeedForwardNetwork(List.of(
            new VectorizedDenseLayer(new float[16][128], new float[16], ActivationFunction.RELU)
        )));
        
        ModelClient compositeClient = new CompositeModelClient(List.of(vectorClient, new SimulatedModelClient()));

        // Setup Executor
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        ExecutorService workerPool = Executors.newFixedThreadPool(8);
        InferenceExecutor executor = new InferenceExecutor(compositeClient, workerPool, scheduler, MetricsRecorder.NOOP);
        ExecutionPlanner planner = new ExecutionPlanner(registry);

        // Prepare Data
        List<Candidate> candidates = new ArrayList<>();
        for (int i = 0; i < candidateCount; i++) {
            float[] vec = new float[128];
            // Introduce some duplicates for dedup testing (half are same)
            if (i % 2 == 0) Arrays.fill(vec, 1.0f);
            candidates.add(new Candidate("c" + i, Map.of("vector", vec), Map.of()));
        }

        RequestContext context = new RequestContext("bench-1", "SEARCH", Instant.now(), Instant.now().plusSeconds(1), Map.of(), candidates, Map.of());
        InferencePlan plan = planner.plan(Set.of("heavy"));

        System.out.println("Running " + iterations + " iterations with " + candidateCount + " candidates...");

        // Warmup
        for (int i = 0; i < 2; i++) {
            executor.execute(plan, context).get();
        }

        long totalTime = 0;
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            InferenceResult result = executor.execute(plan, context).get();
            long end = System.nanoTime();
            totalTime += (end - start);
            
            if (i == 0) {
                System.out.println("Execution Info:");
                System.out.println("  Stages: " + plan.stages().size());
                System.out.println("  Dedup Hits: " + result.executionTrace().events().stream().filter(e -> "DEDUP_HIT".equals(e.eventType())).count());
            }
        }

        long avgLatencyMs = totalTime / iterations / 1_000_000;
        System.out.println("\n--- Benchmark Results ---");
        System.out.println("Avg Optimized Latency: " + avgLatencyMs + "ms");
        
        // Naive Simulation Estimate
        // 100 candidates * (remote light simulation + remote heavy simulation)
        // Without parallel stages, batching or dedup.
        long naiveEstimate = (3 + 8) * candidateCount; 
        System.out.println("Estimated Naive Latency (Serial): ~" + naiveEstimate + "ms");
        
        double speedup = (double) naiveEstimate / avgLatencyMs;
        System.out.printf("Approximate Speedup: %.2fx\n", speedup);

        scheduler.shutdown();
        workerPool.shutdown();
        System.exit(0);
    }
}
