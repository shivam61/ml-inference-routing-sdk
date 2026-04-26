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

## Graceful Degradation
The `ml-routing-vector-inference` module is designed to be optional. If your environment does not support SIMD or if the `--add-modules` flag is missing, you should use the `IN_MEMORY` or `REMOTE` backends. The `VectorizedDenseLayer` itself includes a `computeScalar` method which can be used as a fallback if the Vector API is not available at runtime (though current implementation requires the module to compile).
