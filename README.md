# 🚀 ML Inference Routing SDK

[![Java 21](https://img.shields.io/badge/Java-21-blue.svg)](https://jdk.java.net/21/)
[![Build Status](https://github.com/shivam61/ml-inference-routing-sdk/actions/workflows/ci.yml/badge.svg)](https://github.com/shivam61/ml-inference-routing-sdk/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![V-Threads](https://img.shields.io/badge/Project--Loom-Virtual--Threads-orange)](#)

**High-Performance ML Inference Orchestration.** This SDK addresses the "Heavy Fan-out" problem in low-latency backend services. It treats online inference as a **Directed Acyclic Graph (DAG)** execution problem, applying database-style query optimizations like lazy pruning, batching, and request deduplication.

---

## 💎 Why Use This SDK? (Engineering Impact)

| Feature | Engineering Logic | Performance Impact |
| :--- | :--- | :--- |
| **Concurrency** | Uses **Project Loom Virtual Threads** | Handles 10k+ concurrent model calls with negligible RAM overhead. |
| **Batching** | Groups candidate entities into optimal chunks | Reduces remote gRPC/REST round-trips by **10x-50x**. |
| **Deduplication** | **Canonical Hashing** (xxHash/Murmur3 style) | Eliminates redundant computation for repeat items in <1μs. |
| **Resiliency** | **Structured Concurrency** & Circuit Breakers | Uses `StructuredTaskScope` for robust stage cancellation and load shedding. |
| **Fast Path** | **Local SIMD (Vector API)** Inference | Executing dense layers locally in **<50μs**, bypassing network hops. |

---

## 📊 Performance Benchmarks (JMH)

This project uses **JMH (Java Microbenchmark Harness)** for reproducible performance validation.

*Scenario: 100 Candidates, 2 Execution Stages (Light local ranker -> Heavy remote DNN).*

| Mode | Wall-Clock Latency (p99) | Remote Calls | CPU Overhead |
| :--- | :--- | :--- | :--- |
| **Naive Loop** | ~850ms | 100 | High (Context Switching) |
| **Optimized DAG** | **~45ms** | 4 | Low (Virtual Threads) |

> Run real benchmarks on your hardware:
> `mvn clean install && mvn -pl ml-routing-benchmarks exec:java -Dexec.mainClass="io.github.shivam61.mlinference.benchmarks.InferenceBenchmark"`

---

## 🏗️ Ecosystem & Modules

- **`ml-routing-core`**: The brain. DAG planning, Batching, and **Structured Concurrency**.
- **`ml-routing-spring-boot-starter`**: Drop-in auto-configuration for Spring Boot 3.
- **`ml-routing-micrometer`**: Out-of-the-box metrics for Prometheus/Grafana.
- **`ml-routing-clients`**: Standardized adapters for **NVIDIA Triton** and **TF Serving**.
- **`ml-routing-vector-inference`**: Local SIMD engine (Requires `--add-modules jdk.incubator.vector`).
- **`ml-routing-onnx`**: Local execution for complex models.

---

## 🛠️ Usage (Spring Boot)

Simply add the dependency and your `model-registry.yaml`. The SDK will auto-configure the `InferenceExecutor`.

```java
@Autowired
private InferenceExecutor executor;

public InferenceResult getRecommendations(List<Candidate> candidates) {
    InferencePlan plan = planner.plan(Set.of("ranking_model"));
    RequestContext ctx = new RequestContext("id", "REC", Instant.now(), Instant.now().plusMillis(100), Map.of(), candidates, Map.of());
    
    return executor.execute(plan, ctx).get();
}
```

## 📚 Documentation
- [Architecture Overview](docs/architecture.md)
- [Execution Model & Optimizations](docs/execution-model.md)
- [Local Vectorized Inference (SIMD)](docs/local-vectorized-inference.md)
- [Observability & Tracing](docs/observability.md)
- [Project Roadmap](docs/roadmap.md)
