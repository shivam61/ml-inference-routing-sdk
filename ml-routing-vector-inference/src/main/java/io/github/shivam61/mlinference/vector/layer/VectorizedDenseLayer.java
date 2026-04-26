package io.github.shivam61.mlinference.vector.layer;

import io.github.shivam61.mlinference.vector.activation.ActivationFunction;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

public class VectorizedDenseLayer {
    private static final VectorSpecies<Float> SPECIES = FloatVector.SPECIES_PREFERRED;
    
    private final float[][] weights; // [outputDim][inputDim]
    private final float[] biases;
    private final ActivationFunction activation;

    public VectorizedDenseLayer(float[][] weights, float[] biases, ActivationFunction activation) {
        this.weights = weights;
        this.biases = biases;
        this.activation = activation;
    }

    public float[] compute(float[] input) {
        float[] output = new float[weights.length];
        
        for (int i = 0; i < weights.length; i++) {
            float sum = dotProductVectorized(weights[i], input);
            output[i] = activation.apply(sum + biases[i]);
        }
        
        return output;
    }

    private float dotProductVectorized(float[] w, float[] x) {
        int bound = SPECIES.loopBound(w.length);
        FloatVector sumVector = FloatVector.zero(SPECIES);
        
        int i = 0;
        for (; i < bound; i += SPECIES.length()) {
            FloatVector wv = FloatVector.fromArray(SPECIES, w, i);
            FloatVector xv = FloatVector.fromArray(SPECIES, x, i);
            sumVector = wv.fma(xv, sumVector);
        }
        
        float res = sumVector.reduceLanes(VectorOperators.ADD);
        
        // Scalar fallback for remainder
        for (; i < w.length; i++) {
            res += w[i] * x[i];
        }
        
        return res;
    }

    // Provided for benchmarking comparison
    public float[] computeScalar(float[] input) {
        float[] output = new float[weights.length];
        for (int i = 0; i < weights.length; i++) {
            float sum = 0;
            for (int j = 0; j < weights[i].length; j++) {
                sum += weights[i][j] * input[j];
            }
            output[i] = activation.apply(sum + biases[i]);
        }
        return output;
    }
}
