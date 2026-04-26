# 🚀 ML Inference Routing SDK

[![Java 21](https://img.shields.io/badge/Java-21-blue.svg)](https://jdk.java.net/21/)
[![Build](https://img.shields.io/badge/build-passing-brightgreen)](#)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![V-Threads](https://img.shields.io/badge/Project--Loom-Virtual--Threads-orange)](#)

**Stop the Latency Explosion.** The ML Inference Routing SDK is a Staff-Engineer level orchestration engine designed to handle the "Heavy Fan-out" problem in modern ML architectures. It treats online inference as an **Execution Planning** problem, applying database-style optimizations to your ML pipeline.

---

## 💎 Why Use This SDK?

| Feature | The Naive Approach | **ML Inference Routing SDK** |
| :--- | :--- | :--- |
| **Concurrency** | OS Thread-per-request (Heavy) | **Project Loom Virtual Threads** (Lightweight) |
| **Network** | 1000s of serial network calls | **Smart Batching** & Parallel DAG stages |
| **Redundancy** | Re-computes everything | **Per-Request Deduplication** |
| **Resiliency** | Cascading failures / Timeouts | **Zero-Dep Circuit Breakers** & Fallbacks |
| **Optimization** | Hits the network for every score | **Lazy Pruning** & Local SIMD Inference |

---

## 🧠 Core Engineering Logic

The SDK transforms a simple request into a highly optimized **Execution DAG**:

1.  **Smart Routing:** Evaluates declarative rules to pick the right models.
2.  **DAG Planning:** Topologically sorts models. If Model B needs Model A's output, it waits; otherwise, they run in parallel.
3.  **Deduplication:** Identifies identical feature sets. *Why score the same item twice?* We score it once and share the result.
4.  **Lazy Pruning:** Automatically filters candidates between stages. Use a fast **SIMD-accelerated** local model to drop the bottom 90% before hitting your expensive remote GPU cluster.
5.  **Circuit Breaking:** Proactively sheds load if a model server starts failing, returning safe "neutral" scores to keep your user experience alive.

---

## 🎯 Enterprise Use Cases

### 🔍 1. Multi-Stage Search Ranking
*   **Problem:** 2,000 search results need ranking. A heavy BERT model takes 1s to score all 2,000.
*   **SDK Solution:** Run a fast **Local Vector API** ranker in Stage 0. Prune to the Top 50. Run the BERT model in Stage 1 only for those 50. 
*   **Result:** Deep ranking achieved in **< 40ms**.

### 🛡️ 2. Real-time Fraud & Risk Analysis
*   **Problem:** checkout must hit 5 different fraud models. If one is slow, the user is stuck.
*   **SDK Solution:** Run all 5 models in parallel stages. Enforce a strict 100ms deadline. Use a **Circuit Breaker** to instantly return a "Safe" fallback score if the risk-engine is unstable.
*   **Result:** Zero abandoned checkouts due to ML latency.

### 🏠 3. Massive Scale Personalization
*   **Problem:** Home feed generation involves thousands of redundant user-feature computations.
*   **SDK Solution:** **Deduplication** caches user embeddings within the request boundary, slashing your model-server costs by up to 60%.

---

## 🛠️ Modern Java 21 Usage

Built for **Project Loom**. No more complex `CompletableFuture` chains. The code looks synchronous, but it's blazingly fast and non-blocking.

```java
// Modern, clean, and blazingly fast
try (InferenceExecutor executor = new InferenceExecutor(myClient, MetricsRecorder.NOOP)) {
    
    // 1. Define context (SEARCH, HOME_FEED, etc.)
    RequestContext ctx = new RequestContext("req-1", "SEARCH", Instant.now(), 
        Instant.now().plusMillis(50), Map.of(), myCandidates, Map.of());

    // 2. Execute! (The SDK handles DAG, Batching, Dedup, and Circuit Breaking)
    InferenceResult result = executor.execute(plan, ctx).get();
    
    // 3. Inspect the "Black Box"
    result.executionTrace().events().forEach(System.out::println);
}
```

---

## 📦 Architecture & Modules

-   **`ml-routing-core`**: The brain. DAG planning, Batching, Circuit Breakers, and Virtual Thread Orchestration.
-   **`ml-routing-clients`**: High-performance adaptors for **NVIDIA Triton** and **TF Serving**.
-   **`ml-routing-vector-inference`**: Local **SIMD (Vector API)** engine for lightweight neural layers.
-   **`ml-routing-onnx`**: Local execution of complex pre-trained models via **ONNX Runtime**.

## 🚀 Quick Start

```bash
# Clone and Build
git clone https://github.com/shivam61/ml-inference-routing-sdk.git
mvn clean install

# Run the Staff-Level Search Ranking Demo
mvn -pl ml-routing-examples exec:java -Dexec.mainClass="com.github.placeholder.mlinference.examples.SearchRankingExample"
```

## 🤖 AI Agent Ready
Designed for autonomous integration. AI Agents (Cursor, Windsurf) can use our **[AI Integration Guide](docs/ai-integration.md)** and **`.cursorrules`** to safely extend the framework with zero hallucinations.

## 📚 Documentation
-   [Architecture Overview](docs/architecture.md)
-   [Execution Model & Optimizations](docs/execution-model.md)
-   [Local Vectorized Inference (SIMD)](docs/local-vectorized-inference.md)
-   [Configuration Reference](docs/config-reference.md)
-   [Observability & Tracing](docs/observability.md)
-   [Project Roadmap](docs/roadmap.md)
