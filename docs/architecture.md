# Architecture Overview

The ML Inference Routing SDK is designed as a modular, high-performance library for orchestrating machine learning model inference. It decouples the **what** (model definitions and routing rules) from the **how** (execution planning and asynchronous processing).

## Core Components

### 1. Model Registry
The central repository for all available models. It handles:
- Model metadata management (versioning, timeouts, batch sizes).
- Dependency graph validation (preventing missing dependencies and cycles).
- Backend type configuration (REMOTE, LOCAL_VECTOR, IN_MEMORY).

### 2. Routing Engine
Evaluates incoming `RequestContext` against a set of prioritized `RoutingRule`s.
- Selects the target models for a given request.
- Supports conditional logic based on request attributes and types.

### 3. Execution Planner
Transforms the set of selected models into a valid `InferencePlan`.
- **Topological Sorting:** Groups models into parallel execution stages based on their dependencies.
- **Dependency Resolution:** Automatically includes required upstream models even if they weren't explicitly selected by routing.

### 4. Inference Executor
The engine that realizes the `InferencePlan`.
- **Project Loom (Virtual Threads):** Uses Java 21 `Executors.newVirtualThreadPerTaskExecutor()` to handle high-concurrency without thread-starvation. The entire DAG traversal is non-blocking yet written in a clean, synchronous style.
- **Circuit Breaker:** A zero-dependency resiliency layer that tracks model health. It prevents "waiting for timeouts" by proactively shedding load when a backend is detected as unhealthy.
- **Batching:** Groups candidate requests to minimize network overhead.
- **Deduplication:** Avoids redundant computations for identical feature sets within a single request.
- **Lazy Execution:** Prunes candidates at runtime using predicates.

### 5. Model Clients
Pluggable backends for actual inference.
- `SimulatedModelClient`: For testing and benchmarking.
- `LocalVectorizedModelClient`: High-performance SIMD execution using Java Vector API.
- `TritonModelClient / TFServingModelClient`: Industry-standard remote serving integrations.
- `LocalOnnxModelClient`: Local execution of complex pre-trained models via ONNX Runtime.
- `CompositeModelClient`: Orchestrates routing across multiple client implementations.

## Design Principles
- **Immutability:** All domain objects are immutable Java records.
- **Resiliency:** Enforced timeouts, circuit breakers, and fallback strategies ensure system stability.
- **Modern Concurrency:** Leverages Virtual Threads for scalable I/O.
- **Zero Framework Bloat:** The core module has no dependencies on Spring or other heavy frameworks.
