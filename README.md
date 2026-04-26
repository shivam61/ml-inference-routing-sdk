# ML Inference Routing SDK

![Java 21](https://img.shields.io/badge/Java-21-blue.svg)
![Build](https://img.shields.io/badge/build-passing-brightgreen)
![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)

## 📖 What is this library?
At its core, this is a **traffic controller for Machine Learning models** in Java. If your backend application needs to ask an AI/ML model for predictions, scores, or rankings, this SDK ensures that those requests happen incredibly fast and never crash your application.

## 🛑 The Problem: "Latency Explosion"
Imagine you are building an e-commerce app. A user searches for "sneakers". Your database finds **1,000** matching sneakers. Now, you want to use a Machine Learning (ML) model to score and rank them so the best sneakers show up at the top of the page.

* **The Naive Approach:** You loop through the 1,000 sneakers and send 1,000 separate network requests to your remote ML model.
* **The Result:** The network gets clogged, the remote model gets overwhelmed, and the search takes 5 seconds to load. The user gets frustrated and leaves.

## 💡 The Solution
This SDK solves this problem by treating ML inference like a smart pipeline:
1. **Lazy Pruning (Filtering early):** Instead of sending 1,000 items to a heavy remote model, the SDK first runs a super-fast, lightweight model *locally* on your server. It drops the bottom 900 items, and only sends the top 100 to the heavy remote model.
2. **Batching (Grouping):** Instead of sending 100 individual requests, the SDK groups them into batches, drastically cutting down network traffic.
3. **Deduplication:** If two items have the exact same features, the SDK only calculates the score once and reuses the answer.
4. **Circuit Breaking & Fallbacks:** If the remote model is slow or failing, the SDK "trips a breaker" and returns safe fallback values instantly, preventing your server from getting stuck waiting for a dying service.
5. **Project Loom (Virtual Threads):** Built on Java 21, the SDK uses lightweight virtual threads to handle thousands of concurrent model calls without exhausting your server's memory.

---

## 🛠️ Step-by-Step: How to Use This Library

### Step 1: Define your Models (`model-registry.yaml`)
You tell the SDK what models exist. Now supports **Triton**, **TensorFlow Serving**, and **ONNX**.

```yaml
models:
  - modelId: deep_ranker
    backendType: TRITON        # Or TF_SERVING, LOCAL_ONNX, LOCAL_VECTOR
    timeoutMs: 50
    maxBatchSize: 32
    fallbackStrategy:
      type: CONSTANT_SCORE
      value: 0.5
```

### Step 2: Define your Routing Rules (`routing-rules.yaml`)
```yaml
rules:
  - ruleId: run_search_models
    priority: 100
    enabled: true
    condition:
      requestType: SEARCH
    selectedModels:
      - deep_ranker
```

### Step 3: Run the Java Code (AI Friendly & Modern)
Thanks to Project Loom, you no longer need to manage complex thread pools.

```java
// 1. Setup your Executor (Now with automatic Virtual Thread management)
try (InferenceExecutor executor = new InferenceExecutor(myModelClient, MetricsRecorder.NOOP)) {
    
    // 2. Create a Request
    RequestContext context = new RequestContext("req-123", "SEARCH", Instant.now(), 
        Instant.now().plusMillis(200), Map.of(), myCandidates, Map.of());

    // 3. Execute! (The SDK handles the DAG, batching, and circuit breaking)
    InferenceResult result = executor.execute(plan, context).get();
}
```

---

## 🤖 AI Agent Integration
This SDK is designed to be **AI-agent friendly**. If you are using a tool like Cursor, Windsurf, or a custom agent:
1. **Context-Aware:** Refer to `.cursorrules` in the root for strict architectural guidelines.
2. **Declarative:** Most changes (adding models/rules) happen in YAML, reducing code-generation errors.
3. **Traceable:** Use `InferenceResult.executionTrace()` to let your agent "see" exactly why a model was skipped or why a fallback was triggered.

---

## 📦 What's Inside this Repository?

This is a Maven multi-module project:
- `ml-routing-core`: Core engine (DAG, Batching, Circuit Breaker). Uses **Project Loom**.
- `ml-routing-clients`: High-performance clients for **NVIDIA Triton** and **TF Serving**.
- `ml-routing-onnx`: Local execution for complex models via **ONNX Runtime**.
- `ml-routing-vector-inference`: Local SIMD-accelerated inference via **Java Vector API**.
- `ml-routing-examples`: Fully runnable Java classes.
- `ml-routing-benchmarks`: Speed tests.

## 🚀 Quick Start (Running the Code)

You need **Java 21** and **Maven** installed on your machine.

```bash
# 1. Download and build the project
git clone https://github.com/shivam61/ml-inference-routing-sdk.git
cd ml-inference-routing-sdk
mvn clean install

# 2. Run the main Search Ranking Example
# (This demonstrates local models, remote models, and fallbacks working together)
mvn -pl ml-routing-examples exec:java -Dexec.mainClass="com.github.placeholder.mlinference.examples.SearchRankingExample"

# 3. Run the Deduplication Example
# (This proves that duplicate items are only computed once)
mvn -pl ml-routing-examples exec:java -Dexec.mainClass="com.github.placeholder.mlinference.examples.DedupExample"
```

## 📚 Deep Dive Documentation

If you want to understand the advanced engineering behind this, check out our docs:
- [AI Agent Integration Guide](docs/ai-integration.md)
- [Architecture Design](docs/architecture.md)
- [Execution Model (How Batching/Pruning works)](docs/execution-model.md)
- [Local Vectorized Inference (SIMD)](docs/local-vectorized-inference.md)
- [Configuration Reference](docs/config-reference.md)
- [Observability (Metrics & Tracing)](docs/observability.md)
