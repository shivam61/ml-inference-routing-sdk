# Execution Model

The SDK's execution model is designed to handle high-concurrency, low-latency ML inference by treating the process as a structured DAG (Directed Acyclic Graph) traversal.

## The Execution Lifecycle

1. **Routing:** The `RoutingEngine` identifies which models need to produce results for the current request.
2. **Planning:** The `ExecutionPlanner` builds a topological schedule. Models with no dependencies form Stage 0. Models depending only on Stage 0 form Stage 1, and so on.
3. **Execution:** The `InferenceExecutor` iterates through the stages. Each model within a stage is executed concurrently.

## Optimization Strategies

### Batching
The executor groups multiple candidate inputs into a single call to the `ModelClient`. This is critical for remote backends to minimize network round-trips. Batch sizes are capped by `maxBatchSize` in the `ModelDefinition`.

### Deduplication
Within a single request, multiple candidates might share the same feature vector (e.g., in a search context). The executor computes the inference once and reuses the result for all identical inputs, recording a `DEDUP_HIT` in the trace.

### Lazy Execution
Lazy predicates allow the system to prune candidates before they reach expensive models.
- **Example:** A `deep_ranker` only executes for the Top 10 candidates produced by a `light_ranker`.
- Candidates that are pruned are not sent to the model client, saving significant resources and latency.

## Failure Handling and Resiliency

### Deadlines
The `RequestContext` carries a global deadline. Before starting any model or batch, the executor checks if enough time remains. If the deadline is exceeded, it skips execution and triggers a fallback.

### Timeouts
Each model has a specific `timeoutMs`. If the client does not return a result within this window, the executor cancels the future and applies the model's `FallbackStrategy`.

### Fallback Strategies
- **CONSTANT_SCORE:** Returns a pre-defined score (e.g., 0.5 or 0.0).
- **DEFAULT_OUTPUT:** Uses a static default output defined in the registry.
- **SKIP_MODEL:** Silently skips the model, allowing downstream models to handle the missing input if possible.
- **FAIL_FAST:** Immediately terminates the request with a failure status.
