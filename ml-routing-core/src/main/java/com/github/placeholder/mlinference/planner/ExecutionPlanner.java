package com.github.placeholder.mlinference.planner;

import com.github.placeholder.mlinference.domain.ModelDefinition;
import com.github.placeholder.mlinference.registry.ModelRegistry;
import java.util.*;
import java.util.stream.Collectors;

public class ExecutionPlanner {
    private final ModelRegistry registry;

    public ExecutionPlanner(ModelRegistry registry) {
        this.registry = registry;
    }

    public InferencePlan plan(Set<String> selectedModelIds) {
        // 1. Resolve all models including recursive dependencies
        Set<String> allModelIds = new HashSet<>();
        for (String id : selectedModelIds) {
            resolveDependencies(id, allModelIds);
        }

        List<ModelDefinition> modelsToExecute = allModelIds.stream()
            .map(id -> registry.getModel(id).orElseThrow())
            .toList();

        // 2. Topological sort into stages
        List<InferencePlan.ExecutionStage> stages = buildStages(modelsToExecute);

        return new InferencePlan(stages, selectedModelIds);
    }

    private void resolveDependencies(String modelId, Set<String> resolved) {
        if (resolved.contains(modelId)) return;
        
        ModelDefinition model = registry.getModel(modelId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown model: " + modelId));
            
        resolved.add(modelId);
        for (String dep : model.dependencies()) {
            resolveDependencies(dep, resolved);
        }
    }

    private List<InferencePlan.ExecutionStage> buildStages(List<ModelDefinition> models) {
        Map<String, ModelDefinition> modelMap = models.stream()
            .collect(Collectors.toMap(ModelDefinition::modelId, m -> m));
        
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> adjacencyList = new HashMap<>();

        for (ModelDefinition model : models) {
            inDegree.putIfAbsent(model.modelId(), 0);
            for (String dep : model.dependencies()) {
                // Only consider dependencies that are within our plan
                if (modelMap.containsKey(dep)) {
                    adjacencyList.computeIfAbsent(dep, k -> new ArrayList<>()).add(model.modelId());
                    inDegree.put(model.modelId(), inDegree.getOrDefault(model.modelId(), 0) + 1);
                }
            }
        }

        List<InferencePlan.ExecutionStage> stages = new ArrayList<>();
        Queue<String> queue = new LinkedList<>();
        
        // Find nodes with no incoming edges
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        int stageNum = 0;
        while (!queue.isEmpty()) {
            int size = queue.size();
            List<ModelDefinition> stageModels = new ArrayList<>();
            
            for (int i = 0; i < size; i++) {
                String current = queue.poll();
                stageModels.add(modelMap.get(current));
                
                List<String> neighbors = adjacencyList.get(current);
                if (neighbors != null) {
                    for (String neighbor : neighbors) {
                        inDegree.put(neighbor, inDegree.get(neighbor) - 1);
                        if (inDegree.get(neighbor) == 0) {
                            queue.add(neighbor);
                        }
                    }
                }
            }
            
            stages.add(new InferencePlan.ExecutionStage(stageNum++, stageModels));
        }

        if (stages.stream().mapToInt(s -> s.models().size()).sum() != models.size()) {
            throw new IllegalStateException("Failed to plan all models. Possible cycle or missing dependency.");
        }

        return stages;
    }
}
