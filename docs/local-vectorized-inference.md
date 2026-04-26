# Local Vectorized Inference

For many latency-sensitive applications, lightweight dense neural models (e.g., scoring rankers with 1-3 layers) are more efficiently executed locally within the JVM than via a remote network call.

## Java Vector API (SIMD)

The `ml-routing-vector-inference` module leverages the **Java Vector API (JEP 448)** to perform SIMD (Single Instruction, Multiple Data) operations. This allows the CPU to process multiple floating-point multiplications and additions in a single instruction cycle.

### Key Performance Features
- **Dot Product Optimization:** We use `FloatVector` to perform dot products for dense layers. On modern CPUs (AVX2, AVX-512), this provides a massive speedup over scalar loops.
- **FMA (Fused Multiply-Add):** Utilizing `wv.fma(xv, sumVector)` to reduce rounding errors and improve instruction throughput.
- **Hardware Agnostic:** The API automatically selects the best vector width (e.g., 256-bit or 512-bit) for the host hardware using `FloatVector.SPECIES_PREFERRED`.

## Configuration

To use the vectorized inference engine, ensure the following:

1. **JVM Flag:** You must add the incubator module at runtime:
   ```bash
   --add-modules jdk.incubator.vector
   ```
2. **Model Definition:** Set `backendType: LOCAL_VECTOR` in your `model-registry.yaml`.

## Performance Comparison

| Model Type | Scalar JVM Latency | Vectorized JVM Latency | Remote (Simulated) |
|------------|-------------------|-----------------------|--------------------|
| 128x64 Dense | ~150μs            | ~35μs                 | ~2ms - 5ms         |

*Note: Vectorized inference is most beneficial for medium-sized dense layers. For extremely small layers, the overhead of vector setup might not be worth it; for extremely large models, a dedicated remote GPU serving stack is preferred.*

## Fallback Mechanism
The `VectorizedDenseLayer` includes an automatic scalar fallback for the "remainder" loop when the input dimension is not a perfect multiple of the hardware's vector lane count.
