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

class DeduplicationTest {

    @Test
    void shouldDeduplicateIdenticalInputs() throws Exception {
        ModelClient client = mock(ModelClient.class);
        ModelDefinition m1 = new ModelDefinition("m1", "f", "v", "", BackendType.IN_MEMORY, 100, 10, Set.of(), null, null, Map.of());
        
        // Return only ONE result even if two are asked (simulating we only call for unique ones)
        when(client.predict(eq(m1), anyList(), any())).thenAnswer(invocation -> {
            List<ModelInput> inputs = invocation.getArgument(1);
            return CompletableFuture.completedFuture(List.of(
                new ModelOutput(inputs.get(0).candidateId(), "m1", 0.99, Map.of())
            ));
        });
        when(client.supports(any())).thenReturn(true);

        InferenceExecutor executor = new InferenceExecutor(client, MetricsRecorder.NOOP);
        InferencePlan plan = new InferencePlan(List.of(new InferencePlan.ExecutionStage(0, List.of(m1))), Set.of("m1"));
        
        // Two candidates with identical features
        Map<String, Object> features = Map.of("f1", 1.0);
        List<Candidate> candidates = List.of(
            new Candidate("c1", features, Map.of()),
            new Candidate("c2", features, Map.of())
        );
        
        RequestContext ctx = new RequestContext("req1", "TEST", Instant.now(), null, Map.of(), candidates, Map.of());
        
        InferenceResult result = executor.execute(plan, ctx).get(1, TimeUnit.SECONDS);
        
        // Verify we got 2 outputs total
        assertThat(result.outputsByModel().get("m1")).hasSize(2);
        
        // Verify the trace contains a DEDUP_HIT
        long dedupHits = result.executionTrace().events().stream()
            .filter(e -> "DEDUP_HIT".equals(e.eventType()))
            .count();
        assertThat(dedupHits).isEqualTo(1);
        
        // Verify model was called only ONCE for the unique input
        verify(client, times(1)).predict(eq(m1), anyList(), any());
    }
}
