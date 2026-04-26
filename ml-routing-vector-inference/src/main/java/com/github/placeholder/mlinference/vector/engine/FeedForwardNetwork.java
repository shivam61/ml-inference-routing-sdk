package com.github.placeholder.mlinference.vector.engine;

import com.github.placeholder.mlinference.vector.layer.VectorizedDenseLayer;
import java.util.List;

public class FeedForwardNetwork {
    private final List<VectorizedDenseLayer> layers;

    public FeedForwardNetwork(List<VectorizedDenseLayer> layers) {
        this.layers = layers;
    }

    public float[] predict(float[] input) {
        float[] current = input;
        for (VectorizedDenseLayer layer : layers) {
            current = layer.compute(current);
        }
        return current;
    }
}
