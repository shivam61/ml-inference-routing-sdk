# Project Roadmap

The ML Inference Routing SDK is an evolving project. Future enhancements will focus on deeper integration with local runtimes and more sophisticated execution strategies.

## Short-Term
- [ ] **ONNX Runtime Support:** Add a `LocalOnnxModelClient` for executing more complex models locally using ONNX.
- [ ] **Dynamic Weight Updates:** Allow the `ModelRegistry` to update model weights/parameters at runtime without a JVM restart.
- [ ] **Advanced Lazy Predicates:** Support custom Java predicates for more complex pruning logic (e.g., pruning based on multi-model consensus).

## Medium-Term
- [ ] **Virtual Thread Support:** Explore using Java 21 Virtual Threads (Project Loom) for the I/O-bound `ModelClient` calls to reduce the overhead of large fixed thread pools.
- [ ] **Streaming Inference:** Support models that return streaming outputs (e.g., LLMs).
- [ ] **Automatic Batch Tuning:** Dynamically adjust `maxBatchSize` based on observed latency histograms and throughput.

## Long-Term
- [ ] **Distributed Execution:** Allow the DAG to be partially executed across multiple microservices while maintaining a single logical execution trace.
- [ ] **Hardware Acceleration:** Deeper integration with GPU/TPU backends for the local vector module.
