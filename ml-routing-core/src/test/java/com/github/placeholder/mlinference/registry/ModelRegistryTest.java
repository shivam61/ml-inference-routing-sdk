package com.github.placeholder.mlinference.registry;

import com.github.placeholder.mlinference.domain.BackendType;
import com.github.placeholder.mlinference.domain.ModelDefinition;
import org.junit.jupiter.api.Test;
import java.util.Set;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class ModelRegistryTest {

    @Test
    void shouldRegisterAndRetrieveModel() {
        ModelRegistry registry = new ModelRegistry();
        ModelDefinition model = new ModelDefinition("m1", "family", "v1", BackendType.IN_MEMORY, 10, 1, Set.of(), null, null, Map.of());
        
        registry.register(model);
        
        assertThat(registry.getModel("m1")).isPresent().contains(model);
    }

    @Test
    void shouldThrowExceptionOnDuplicateId() {
        ModelRegistry registry = new ModelRegistry();
        ModelDefinition model = new ModelDefinition("m1", "family", "v1", BackendType.IN_MEMORY, 10, 1, Set.of(), null, null, Map.of());
        registry.register(model);
        
        assertThatThrownBy(() -> registry.register(model))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Duplicate model ID");
    }

    @Test
    void shouldValidateMissingDependency() {
        ModelRegistry registry = new ModelRegistry();
        ModelDefinition model = new ModelDefinition("m1", "family", "v1", BackendType.IN_MEMORY, 10, 1, Set.of("missing"), null, null, Map.of());
        registry.register(model);
        
        assertThatThrownBy(registry::validate)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("missing dependency");
    }

    @Test
    void shouldDetectCycles() {
        ModelRegistry registry = new ModelRegistry();
        registry.register(new ModelDefinition("m1", "f", "v", BackendType.IN_MEMORY, 10, 1, Set.of("m2"), null, null, Map.of()));
        registry.register(new ModelDefinition("m2", "f", "v", BackendType.IN_MEMORY, 10, 1, Set.of("m1"), null, null, Map.of()));
        
        assertThatThrownBy(registry::validate)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Dependency cycle detected");
    }
}
