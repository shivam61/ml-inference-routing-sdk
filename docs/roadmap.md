# Project Roadmap

The ML Inference Routing SDK is a high-performance orchestration engine. We have completed the foundational enterprise features and are now focused on ecosystem expansion.

## ✅ Completed Milestones
- [x] **Project Loom Integration:** Refactored core executor to use Java 21 Virtual Threads and Structured Concurrency.
- [x] **Circuit Breaker:** Implemented zero-dependency per-model load shedding and health tracking.
- [x] **Industry Backends:** Added support for NVIDIA Triton (gRPC) and TensorFlow Serving.
- [x] **ONNX Runtime Support:** Added `LocalOnnxModelClient` for local execution of complex models.
- [x] **Local Vector API:** High-performance SIMD inference for dense layers.
- [x] **DAG Orchestration:** Topological planning, batching, and deduplication.

## 🚀 Future Enhancements

### Short-Term
- [ ] **Dynamic Weight Updates:** Allow the `ModelRegistry` to update model weights/parameters at runtime without a JVM restart.
- [ ] **Advanced Lazy Predicates:** Support custom Java predicates for more complex pruning logic (e.g., pruning based on multi-model consensus).
- [ ] **Observability Export:** Add official exporters for Micrometer / OpenTelemetry.

### Medium-Term
- [ ] **Streaming Inference:** Support models that return streaming outputs (e.g., LLMs).
- [ ] **Automatic Batch Tuning:** Dynamically adjust `maxBatchSize` based on observed latency histograms and throughput.

### Long-Term
- [ ] **Distributed Execution:** Allow the DAG to be partially executed across multiple microservices while maintaining a single logical execution trace.
- [ ] **Hardware Acceleration:** Deeper integration with GPU/TPU backends for the local vector module.
