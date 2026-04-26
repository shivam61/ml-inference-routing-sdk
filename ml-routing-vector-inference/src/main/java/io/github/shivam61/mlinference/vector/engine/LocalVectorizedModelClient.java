package io.github.shivam61.mlinference.vector.engine;

import io.github.shivam61.mlinference.client.ModelClient;
import io.github.shivam61.mlinference.domain.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class LocalVectorizedModelClient implements ModelClient {
    private final Map<String, FeedForwardNetwork> networks = new ConcurrentHashMap<>();

    public void registerModel(String modelId, FeedForwardNetwork network) {
        networks.put(modelId, network);
    }

    @Override
    public CompletableFuture<List<ModelOutput>> predict(ModelDefinition model, List<ModelInput> inputs, RequestContext ctx) {
        FeedForwardNetwork network = networks.get(model.modelId());
        if (network == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Local model not found: " + model.modelId()));
        }

        return CompletableFuture.supplyAsync(() -> inputs.stream()
            .map(input -> {
                float[] featureVector = extractFeatures(input.features());
                float[] result = network.predict(featureVector);
                // Assume first element is the score for simplicity
                double score = result.length > 0 ? result[0] : 0.0;
                return new ModelOutput(input.candidateId(), model.modelId(), score, Map.of("backend", "LOCAL_VECTOR"));
            })
            .toList());
    }

    private float[] extractFeatures(Map<String, Object> features) {
        Object raw = features.get("vector");
        if (raw instanceof float[] f) return f;
        if (raw instanceof List<?> list) {
            float[] f = new float[list.size()];
            for (int i = 0; i < list.size(); i++) {
                f[i] = ((Number) list.get(i)).floatValue();
            }
            return f;
        }
        
        // Default fallback: collect all numeric values sorted by key
        return features.entrySet().stream()
            .filter(e -> e.getValue() instanceof Number)
            .sorted(Map.Entry.comparingByKey())
            .map(e -> ((Number) e.getValue()).floatValue())
            .collect(() -> new FloatList(), FloatList::add, FloatList::addAll)
            .toArray();
    }

    @Override
    public boolean supports(BackendType backendType) {
        return backendType == BackendType.LOCAL_VECTOR;
    }

    private static class FloatList {
        private float[] data = new float[16];
        private int size = 0;
        public void add(float f) {
            if (size == data.length) {
                float[] next = new float[data.length * 2];
                System.arraycopy(data, 0, next, 0, data.length);
                data = next;
            }
            data[size++] = f;
        }
        public void addAll(FloatList other) {
            for (int i = 0; i < other.size; i++) add(other.data[i]);
        }
        public float[] toArray() {
            float[] res = new float[size];
            System.arraycopy(data, 0, res, 0, size);
            return res;
        }
    }
}
