# Project Roadmap

The ML Inference Routing SDK is a high-performance orchestration engine. We have completed the foundational enterprise features and are now focused on ecosystem expansion.

## ✅ Completed Milestones
- [x] **Structured Concurrency:** Refactored core executor to use Java 21 `StructuredTaskScope` (JEP 453).
- [x] **Project Loom Integration:** Uses Virtual Threads for scalable I/O.
- [x] **Micrometer Integration:** New module `ml-routing-micrometer` for Prometheus/Grafana.
- [x] **Spring Boot 3 Support:** New `ml-routing-spring-boot-starter` for auto-configuration.
- [x] **High-Performance Hashing:** Optimized `FeatureHasher` for sub-microsecond deduplication.
- [x] **JMH Benchmarking:** Reproducible performance harness in `ml-routing-benchmarks`.
- [x] **Circuit Breaker:** Zero-dependency per-model load shedding and health tracking.
- [x] **Industry Backends:** Support for NVIDIA Triton (gRPC) and TensorFlow Serving.
- [x] **ONNX Runtime Support:** Added `LocalOnnxModelClient` for local execution of complex models.

## 🚀 Future Enhancements

### Short-Term
- [ ] **Reactor/Mutiny Adapters:** Add thin wrappers for project Reactor and Mutiny for seamless integration with WebFlux and Quarkus.
- [ ] **OpenTelemetry Exporter:** Direct export of `ExecutionTrace` to OTel collectors (Jaeger, Honeycomb).
- [ ] **Dynamic Weight Updates:** Allow the `ModelRegistry` to update model weights/parameters at runtime without a JVM restart.

### Medium-Term
- [ ] **Streaming Inference:** Support models that return streaming outputs (e.g., LLMs).
- [ ] **Automatic Batch Tuning:** Dynamically adjust `maxBatchSize` based on observed latency histograms and throughput.

### Long-Term
- [ ] **Distributed Execution:** Allow the DAG to be partially executed across multiple microservices while maintaining a single logical execution trace.
- [ ] **Hardware Acceleration:** Deeper integration with GPU/TPU backends for the local vector module.
