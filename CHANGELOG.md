# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0-SNAPSHOT] - 2026-04-26

### Added
- Core DAG-based execution engine for ML inference.
- Support for multiple backends: LOCAL_VECTOR (SIMD), LOCAL_ONNX, TRITON, TF_SERVING.
- Smart batching and deduplication logic.
- Circuit breaker and deadline propagation for resiliency.
- Observability via pluggable MetricsRecorder and ExecutionTrace.
- Project Loom (Virtual Threads) integration for high-concurrency I/O.
- Performance benchmarks and usage examples.
