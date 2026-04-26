package io.github.shivam61.mlinference.client;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import io.github.shivam61.mlinference.domain.BackendType;
import io.github.shivam61.mlinference.domain.ModelDefinition;
import io.github.shivam61.mlinference.domain.ModelInput;
import io.github.shivam61.mlinference.domain.ModelOutput;
import io.github.shivam61.mlinference.domain.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Local ONNX Runtime model client.
 */
public class LocalOnnxModelClient implements ModelClient, AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(LocalOnnxModelClient.class);
    private final OrtEnvironment env;
    private final Map<String, OrtSession> sessions = new ConcurrentHashMap<>();

    public LocalOnnxModelClient() {
        this.env = OrtEnvironment.getEnvironment();
    }

    @Override
    public CompletableFuture<List<ModelOutput>> predict(
            ModelDefinition model,
            List<ModelInput> inputs,
            RequestContext ctx) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                OrtSession session = getOrCreateSession(model);
                return executeInference(session, inputs);
            } catch (OrtException e) {
                logger.error("ONNX inference failed for model {}", model.modelId(), e);
                throw new RuntimeException("ONNX inference failed", e);
            }
        });
    }

    private OrtSession getOrCreateSession(ModelDefinition model) throws OrtException {
        return sessions.computeIfAbsent(model.modelId(), id -> {
            try {
                logger.info("Initializing ONNX session for model {} at {}", id, model.endpoint());
                return env.createSession(model.endpoint(), new OrtSession.SessionOptions());
            } catch (OrtException e) {
                throw new RuntimeException("Failed to create ONNX session", e);
            }
        });
    }

    private List<ModelOutput> executeInference(OrtSession session, List<ModelInput> inputs) throws OrtException {
        // In a real implementation, we would batch inputs into tensors
        // This is a skeleton demonstrating the translation logic
        
        return inputs.stream().map(input -> {
            try {
                Map<String, OnnxTensor> container = new HashMap<>();
                for (Map.Entry<String, Object> entry : input.features().entrySet()) {
                    // Simplified tensor creation for demonstration
                    if (entry.getValue() instanceof float[] values) {
                        container.put(entry.getKey(), OnnxTensor.createTensor(env, values));
                    }
                }

                try (OrtSession.Result results = session.run(container)) {
                    // Extract output (simulated)
                    double score = 0.75; 
                    return new ModelOutput(input.candidateId(), score, Map.of("backend", "local_onnx"));
                } finally {
                    for (OnnxTensor t : container.values()) {
                        t.close();
                    }
                }
            } catch (OrtException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
    }

    @Override
    public boolean supports(BackendType backendType) {
        return backendType == BackendType.LOCAL_ONNX;
    }

    @Override
    public void close() {
        for (OrtSession session : sessions.values()) {
            try {
                session.close();
            } catch (OrtException e) {
                logger.error("Error closing ONNX session", e);
            }
        }
        env.close();
    }
}
