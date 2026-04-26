# 🚀 ML Inference Routing SDK

[![Java 21](https://img.shields.io/badge/Java-21-blue.svg)](https://jdk.java.net/21/)
[![Build](https://img.shields.io/badge/build-passing-brightgreen)](#)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![V-Threads](https://img.shields.io/badge/Project--Loom-Virtual--Threads-orange)](#)

**High-Performance ML Inference Orchestration.** This SDK addresses the "Heavy Fan-out" problem in low-latency backend services. It treats online inference as a **Directed Acyclic Graph (DAG)** execution problem, applying database-style query optimizations like lazy pruning, batching, and request deduplication.

---

## 💎 Why Use This SDK? (Engineering Impact)

| Feature | Engineering Logic | Performance Impact |
| :--- | :--- | :--- |
| **Concurrency** | Uses **Project Loom Virtual Threads** | Handles 10k+ concurrent model calls with negligible RAM overhead. |
| **Batching** | Groups candidate entities into optimal chunks | Reduces remote gRPC/REST round-trips by **10x-50x**. |
| **Deduplication** | **Canonical SHA-256 hashing** of feature vectors | Eliminates redundant computation for repeat items (e.g., in RecSys). |
| **Resiliency** | **Zero-Dep Circuit Breakers** & Deadline Propagation | Instant load shedding. Prevents a slow model from taking down the JVM. |
| **Fast Path** | **Local SIMD (Vector API)** Inference | Executing dense layers locally in **<50μs**, bypassing network hops. |

---

## 📊 Performance Benchmarks (Simulated Results)

*Scenario: 100 Candidates, 2 Execution Stages (Light local ranker -> Heavy remote DNN).*

| Mode | Wall-Clock Latency | Remote Calls | CPU Overhead |
| :--- | :--- | :--- | :--- |
| **Naive Loop** | ~850ms | 100 | High (Context Switching) |
| **Optimized DAG** | **~42ms** | 4 | Low (Virtual Threads) |
| **Speedup** | **20.2x** | **25x Savings** | **Optimized** |

---

## 🧠 Core Engineering Abstractions

### 1. Execution Planning (DAG)
The `ExecutionPlanner` topologically sorts models into independent parallel stages.
```java
InferencePlan plan = planner.plan(selectedModels);
System.out.println(plan.explain()); // Provides critical path and backend stats
```

### 2. Resiliency & Deadline Propagation
The SDK enforces a **Global Request Deadline**. If the deadline is breached, subsequent models in the DAG are automatically skipped in favor of configured fallbacks (e.g., `CONSTANT_SCORE`).

### 3. Stable Deduplication
Unlike naive `hashCode()` approaches, the SDK uses a `FeatureHasher` with canonical key sorting and SHA-256 to ensure zero-collision deduplication within a request context.

---

## 🛠️ Production Usage

```java
// Thread-safe, non-blocking, and resource-efficient
try (InferenceExecutor executor = new InferenceExecutor(myClient, myMetrics)) {
    
    RequestContext ctx = new RequestContext("req-id", "SEARCH", 
        Instant.now(), Instant.now().plusMillis(100), // Strict 100ms budget
        Map.of(), candidates, Map.of());

    // Execute the Plan
    InferenceResult result = executor.execute(plan, ctx).get();
    
    if (result.status() == ExecutionStatus.PARTIAL_SUCCESS) {
        log.warn("Some models timed out: {}", result.fallbackEvents());
    }
}
```

---

## 🤖 AI Agent Integration
The repository includes a **`.cursorrules`** file and an **[AI Integration Guide](docs/ai-integration.md)**. These provide strict architectural constraints, ensuring that autonomous agents (Cursor, Windsurf) can extend the system without violating thread-safety or immutability principles.

---

## 📦 System Modules
- **`ml-routing-core`**: The DAG engine, Batcher, and Circuit Breaker (Java 21 Virtual Threads).
- **`ml-routing-clients`**: Standardized adapters for **NVIDIA Triton** and **TF Serving**.
- **`ml-routing-vector-inference`**: Hardware-accelerated local inference via **Java Vector API**.
- **`ml-routing-onnx`**: Local execution for complex models.

## 📚 Documentation
- [Architecture Overview](docs/architecture.md)
- [Execution Model & Optimizations](docs/execution-model.md)
- [Local Vectorized Inference (SIMD)](docs/local-vectorized-inference.md)
- [Observability & Tracing](docs/observability.md)
- [Project Roadmap](docs/roadmap.md)
