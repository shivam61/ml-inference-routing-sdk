package com.github.placeholder.mlinference.registry;

import com.github.placeholder.mlinference.domain.ModelDefinition;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ModelRegistry {
    private final Map<String, ModelDefinition> models = new ConcurrentHashMap<>();

    public void register(ModelDefinition model) {
        if (models.containsKey(model.modelId())) {
            throw new IllegalArgumentException("Duplicate model ID: " + model.modelId());
        }
        models.put(model.modelId(), model);
    }

    public Optional<ModelDefinition> getModel(String modelId) {
        return Optional.ofNullable(models.get(modelId));
    }

    public Collection<ModelDefinition> getAllModels() {
        return Collections.unmodifiableCollection(models.values());
    }

    public void validate() {
        // 1. Validate dependencies exist
        for (ModelDefinition model : models.values()) {
            for (String dep : model.dependencies()) {
                if (!models.containsKey(dep)) {
                    throw new IllegalStateException("Model " + model.modelId() + " has missing dependency: " + dep);
                }
            }
        }

        // 2. Detect cycles
        detectCycles();
    }

    private void detectCycles() {
        Set<String> visited = new HashSet<>();
        Set<String> stack = new HashSet<>();

        for (String modelId : models.keySet()) {
            if (hasCycle(modelId, visited, stack)) {
                throw new IllegalStateException("Dependency cycle detected involving model: " + modelId);
            }
        }
    }

    private boolean hasCycle(String current, Set<String> visited, Set<String> stack) {
        if (stack.contains(current)) return true;
        if (visited.contains(current)) return false;

        visited.add(current);
        stack.add(current);

        ModelDefinition model = models.get(current);
        if (model != null) {
            for (String dep : model.dependencies()) {
                if (hasCycle(dep, visited, stack)) return true;
            }
        }

        stack.remove(current);
        return false;
    }
}
