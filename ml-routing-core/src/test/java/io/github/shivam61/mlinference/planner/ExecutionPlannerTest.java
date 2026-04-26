package io.github.shivam61.mlinference.planner;

import io.github.shivam61.mlinference.domain.BackendType;
import io.github.shivam61.mlinference.domain.ModelDefinition;
import io.github.shivam61.mlinference.registry.ModelRegistry;
import org.junit.jupiter.api.Test;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutionPlannerTest {

    @Test
    void shouldPlanInStages() {
        ModelRegistry registry = new ModelRegistry();
        ModelDefinition m1 = new ModelDefinition("m1", "f", "v", "", BackendType.IN_MEMORY, 10, 1, Set.of(), null, null, Map.of());
        ModelDefinition m2 = new ModelDefinition("m2", "f", "v", "", BackendType.IN_MEMORY, 10, 1, Set.of("m1"), null, null, Map.of());
        ModelDefinition m3 = new ModelDefinition("m3", "f", "v", "", BackendType.IN_MEMORY, 10, 1, Set.of("m1"), null, null, Map.of());
        ModelDefinition m4 = new ModelDefinition("m4", "f", "v", "", BackendType.IN_MEMORY, 10, 1, Set.of("m2", "m3"), null, null, Map.of());
        
        registry.register(m1);
        registry.register(m2);
        registry.register(m3);
        registry.register(m4);

        ExecutionPlanner planner = new ExecutionPlanner(registry);
        InferencePlan plan = planner.plan(Set.of("m4"));

        assertThat(plan.stages()).hasSize(3);
        assertThat(plan.stages().get(0).models()).extracting(ModelDefinition::modelId).containsExactly("m1");
        assertThat(plan.stages().get(1).models()).extracting(ModelDefinition::modelId).containsExactlyInAnyOrder("m2", "m3");
        assertThat(plan.stages().get(2).models()).extracting(ModelDefinition::modelId).containsExactly("m4");
    }
}
