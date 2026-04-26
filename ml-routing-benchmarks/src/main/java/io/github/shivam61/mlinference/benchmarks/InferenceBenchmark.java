package io.github.shivam61.mlinference.benchmarks;

import io.github.shivam61.mlinference.client.*;
import io.github.shivam61.mlinference.domain.*;
import io.github.shivam61.mlinference.executor.InferenceExecutor;
import io.github.shivam61.mlinference.observability.MetricsRecorder;
import io.github.shivam61.mlinference.planner.ExecutionPlanner;
import io.github.shivam61.mlinference.planner.InferencePlan;
import io.github.shivam61.mlinference.registry.ModelRegistry;
import io.github.shivam61.mlinference.vector.activation.ActivationFunction;
import io.github.shivam61.mlinference.vector.engine.FeedForwardNetwork;
import io.github.shivam61.mlinference.vector.engine.LocalVectorizedModelClient;
import io.github.shivam61.mlinference.vector.layer.VectorizedDenseLayer;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Fork(1)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
public class InferenceBenchmark {

    private InferenceExecutor executor;
    private InferencePlan plan;
    private RequestContext context;

    @Setup
    public void setup() {
        ModelRegistry registry = new ModelRegistry();
        ModelDefinition lightModel = new ModelDefinition("light", "rank", "v1", "", BackendType.LOCAL_VECTOR, 5, 100, Set.of(), null, null, Map.of());
        ModelDefinition heavyModel = new ModelDefinition("heavy", "rank", "v1", "", BackendType.REMOTE, 20, 32, Set.of("light"), null, null, Map.of());
        registry.register(lightModel);
        registry.register(heavyModel);

        LocalVectorizedModelClient vectorClient = new LocalVectorizedModelClient();
        vectorClient.registerModel("light", new FeedForwardNetwork(List.of(
            new VectorizedDenseLayer(new float[16][128], new float[16], ActivationFunction.RELU)
        )));
        
        ModelClient compositeClient = new CompositeModelClient(List.of(vectorClient, new SimulatedModelClient()));
        executor = new InferenceExecutor(compositeClient, MetricsRecorder.NOOP);
        
        ExecutionPlanner planner = new ExecutionPlanner(registry);
        plan = planner.plan(Set.of("heavy"));

        List<Candidate> candidates = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            float[] vec = new float[128];
            if (i % 2 == 0) Arrays.fill(vec, 1.0f);
            candidates.add(new Candidate("c" + i, Map.of("vector", vec), Map.of()));
        }
        context = new RequestContext("bench", "SEARCH", Instant.now(), Instant.now().plusSeconds(1), Map.of(), candidates, Map.of());
    }

    @TearDown
    public void tearDown() {
        executor.close();
    }

    @Benchmark
    public InferenceResult testOptimizedExecution() throws Exception {
        return executor.execute(plan, context).get();
    }

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(InferenceBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
}
