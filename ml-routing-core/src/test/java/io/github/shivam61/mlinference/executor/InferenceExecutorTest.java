package io.github.shivam61.mlinference.executor;

import io.github.shivam61.mlinference.client.ModelClient;
import io.github.shivam61.mlinference.domain.*;
import io.github.shivam61.mlinference.observability.MetricsRecorder;
import io.github.shivam61.mlinference.planner.InferencePlan;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InferenceExecutorTest {

    @Test
    void shouldExecuteSimplePlan() throws Exception {
        ModelClient client = mock(ModelClient.class);
        ModelDefinition m1 = new ModelDefinition("m1", "f", "v", "", BackendType.IN_MEMORY, 100, 10, Set.of(), null, null, Map.of());
        
        when(client.predict(eq(m1), anyList(), any())).thenReturn(CompletableFuture.completedFuture(List.of(
            new ModelOutput("c1", "m1", 0.5, Map.of())
        )));
        when(client.supports(any())).thenReturn(true);

        InferenceExecutor executor = new InferenceExecutor(client, MetricsRecorder.NOOP);
        InferencePlan plan = new InferencePlan(List.of(new InferencePlan.ExecutionStage(0, List.of(m1))), Set.of("m1"));
        
        RequestContext ctx = new RequestContext("req1", "TEST", Instant.now(), null, Map.of(), List.of(new Candidate("c1", Map.of(), Map.of())), Map.of());
        
        InferenceResult result = executor.execute(plan, ctx).get(1, TimeUnit.SECONDS);
        
        assertThat(result.status()).isEqualTo(ExecutionStatus.SUCCESS);
        assertThat(result.outputsByModel().get("m1")).hasSize(1);
    }

    @Test
    void shouldTriggerFallbackOnTimeout() throws Exception {
        ModelClient client = mock(ModelClient.class);
        // Set a very short timeout
        ModelDefinition m1 = new ModelDefinition("m1", "f", "v", "", BackendType.IN_MEMORY, 1, 10, Set.of(), 
            new ModelDefinition.FallbackConfig(FallbackType.CONSTANT_SCORE, 0.0, Map.of()), null, Map.of());
        
        CompletableFuture<List<ModelOutput>> slowFuture = new CompletableFuture<>();
        when(client.predict(eq(m1), anyList(), any())).thenReturn(slowFuture);
        when(client.supports(any())).thenReturn(true);

        InferenceExecutor executor = new InferenceExecutor(client, MetricsRecorder.NOOP);
        InferencePlan plan = new InferencePlan(List.of(new InferencePlan.ExecutionStage(0, List.of(m1))), Set.of("m1"));
        
        RequestContext ctx = new RequestContext("req1", "TEST", Instant.now(), null, Map.of(), List.of(new Candidate("c1", Map.of(), Map.of())), Map.of());
        
        InferenceResult result = executor.execute(plan, ctx).get(1, TimeUnit.SECONDS);
        
        assertThat(result.status()).isEqualTo(ExecutionStatus.PARTIAL_SUCCESS);
        assertThat(result.fallbackEvents()).hasSize(1);
        assertThat(result.fallbackEvents().get(0).type()).isEqualTo(FallbackType.CONSTANT_SCORE);
    }
}
