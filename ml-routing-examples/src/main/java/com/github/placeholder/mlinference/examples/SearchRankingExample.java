package com.github.placeholder.mlinference.examples;

import com.github.placeholder.mlinference.client.*;
import com.github.placeholder.mlinference.config.ConfigurationLoader;
import com.github.placeholder.mlinference.domain.*;
import com.github.placeholder.mlinference.executor.InferenceExecutor;
import com.github.placeholder.mlinference.observability.MetricsRecorder;
import com.github.placeholder.mlinference.planner.ExecutionPlanner;
import com.github.placeholder.mlinference.planner.InferencePlan;
import com.github.placeholder.mlinference.registry.ModelRegistry;
import com.github.placeholder.mlinference.routing.RoutingEngine;
import com.github.placeholder.mlinference.vector.activation.ActivationFunction;
import com.github.placeholder.mlinference.vector.engine.FeedForwardNetwork;
import com.github.placeholder.mlinference.vector.engine.LocalVectorizedModelClient;
import com.github.placeholder.mlinference.vector.layer.VectorizedDenseLayer;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class SearchRankingExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== ML Inference Routing SDK: Search Ranking Example ===");

        // 1. Load Configuration
        ModelRegistry registry = new ModelRegistry();
        ConfigurationLoader.loadModels(SearchRankingExample.class.getResourceAsStream("/model-registry.yaml"))
            .forEach(registry::register);
        registry.validate();

        RoutingEngine routingEngine = new RoutingEngine(
            ConfigurationLoader.loadRules(SearchRankingExample.class.getResourceAsStream("/routing-rules.yaml"))
        );

        // 2. Setup Clients
        LocalVectorizedModelClient vectorClient = new LocalVectorizedModelClient();
        // Setup a dummy network for light_ranker: input=4, output=1
        vectorClient.registerModel("light_ranker", new FeedForwardNetwork(List.of(
            new VectorizedDenseLayer(new float[][]{{0.1f, 0.2f, 0.3f, 0.4f}}, new float[]{0.1f}, ActivationFunction.RELU)
        )));

        ModelClient client = new CompositeModelClient(List.of(
            vectorClient,
            new SimulatedModelClient(),
            new ModelClient() {
                @Override
                public java.util.concurrent.CompletableFuture<List<ModelOutput>> predict(ModelDefinition model, List<ModelInput> inputs, RequestContext ctx) {
                    return java.util.concurrent.CompletableFuture.completedFuture(inputs.stream()
                        .map(i -> new ModelOutput(i.candidateId(), model.modelId(), 0.99, Map.of("type", "aggregation")))
                        .toList());
                }
                @Override public boolean supports(BackendType bt) { return bt == BackendType.IN_MEMORY; }
            }
        ));

        // 3. Setup Executor
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        InferenceExecutor executor = new InferenceExecutor(client, Executors.newFixedThreadPool(4), scheduler, MetricsRecorder.NOOP);
        ExecutionPlanner planner = new ExecutionPlanner(registry);

        // 4. Create Request
        List<Candidate> candidates = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            candidates.add(new Candidate("c" + i, Map.of("vector", new float[]{0.1f, 0.5f, 0.2f, 0.8f}), Map.of()));
        }

        RequestContext context = new RequestContext(
            "req-123",
            "SEARCH",
            Instant.now(),
            Instant.now().plusMillis(100),
            Map.of(),
            candidates,
            Map.of()
        );

        // 5. Execute
        Set<String> selectedModels = routingEngine.route(context);
        System.out.println("Selected Models: " + selectedModels);

        InferencePlan plan = planner.plan(selectedModels);
        System.out.println("Execution Plan Stages: " + plan.stages().size());
        for (InferencePlan.ExecutionStage stage : plan.stages()) {
            System.out.println("  Stage " + stage.stageNumber() + ": " + stage.models().stream().map(ModelDefinition::modelId).toList());
        }

        executor.execute(plan, context).thenAccept(result -> {
            System.out.println("\n--- Execution Result ---");
            System.out.println("Status: " + result.status());
            System.out.println("Total Latency: " + result.totalLatencyMs() + "ms");
            System.out.println("Models executed: " + result.outputsByModel().keySet());
            
            List<ModelOutput> finalScores = result.outputsByModel().get("final_score");
            System.out.println("Final score count: " + (finalScores != null ? finalScores.size() : 0));
            
            System.out.println("\nTrace Events:");
            result.executionTrace().events().forEach(e -> 
                System.out.println("  [" + e.timestamp() + "] " + e.eventType() + " " + (e.modelId() != null ? e.modelId() : "")));
            
            System.exit(0);
        }).exceptionally(ex -> {
            ex.printStackTrace();
            System.exit(1);
            return null;
        });
    }
}
