package io.github.shivam61.mlinference.vector.layer;

import io.github.shivam61.mlinference.vector.activation.ActivationFunction;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class VectorizedDenseLayerTest {

    @Test
    void shouldProduceSameResultAsScalar() {
        float[][] weights = {{0.1f, 0.2f, 0.3f, 0.4f, 0.5f}};
        float[] biases = {0.1f};
        VectorizedDenseLayer layer = new VectorizedDenseLayer(weights, biases, ActivationFunction.LINEAR);
        
        float[] input = {1.0f, 1.0f, 1.0f, 1.0f, 1.0f};
        
        float[] vectorizedResult = layer.compute(input);
        float[] scalarResult = layer.computeScalar(input);
        
        assertThat(vectorizedResult[0]).isCloseTo(scalarResult[0], within(1e-6f));
        assertThat(vectorizedResult[0]).isEqualTo(1.6f);
    }
}
