# ML Inference Routing SDK Documentation

Welcome to the official documentation for the **ML Inference Routing SDK**. This high-performance Java 21 library is designed for latency-critical backend services that need to orchestrate complex machine learning inference pipelines.

## 🚀 Getting Started
- **[Installation & Quick Start](../README.md)**: Build the project and run your first example.
- **[Architecture Overview](architecture.md)**: Understand the core components (Planner, Executor, Registry).
- **[Execution Model](execution-model.md)**: Learn about DAG planning, batching, and pruning.

## 🛠️ Configuration & Integration
- **[Config Reference](config-reference.md)**: Detailed guide on `model-registry.yaml` and `routing-rules.yaml`.
- **[Spring Boot Integration](../ml-routing-spring-boot-starter/README.md)**: How to use the SDK in a Spring Boot environment.
- **[AI Agent Integration](ai-integration.md)**: Guidelines for using autonomous agents with this SDK.

## ⚡ Performance & Hardware
- **[Local Vectorized Inference](local-vectorized-inference.md)**: Leveraging Java Vector API (SIMD) for ultra-low latency.
- **[ONNX Runtime Support](architecture.md#5-model-clients)**: Local execution of complex pre-trained models.

## 📈 Monitoring & Resiliency
- **[Observability & Metrics](observability.md)**: Tracing requests and exporting metrics to Micrometer/Prometheus.
- **[Circuit Breaking & Fallbacks](execution-model.md#failure-handling-and-resiliency)**: Keeping your system alive when models fail.

---

## 🗺️ Future
- **[Project Roadmap](roadmap.md)**: See what's coming next for the SDK.
