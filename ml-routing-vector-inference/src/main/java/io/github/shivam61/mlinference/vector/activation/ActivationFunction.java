package io.github.shivam61.mlinference.vector.activation;

public enum ActivationFunction {
    RELU,
    SIGMOID,
    LINEAR;

    public float apply(float x) {
        return switch (this) {
            case RELU -> Math.max(0, x);
            case SIGMOID -> (float) (1.0 / (1.0 + Math.exp(-x)));
            case LINEAR -> x;
        };
    }
}
