# Observability

The SDK provides built-in support for metrics and structured tracing to help debug latency and reliability in production.

## Metrics

Implement the `MetricsRecorder` interface to export metrics to your monitoring system (Prometheus, CloudWatch, etc.).

| Metric Name | Description | Tags |
|-------------|-------------|------|
| `inference.model.call.count` | Total requests to a model | `modelId`, `status` |
| `inference.model.latency` | Latency of model client calls | `modelId` |
| `inference.model.batch_size` | Histogram of batch sizes sent to client | `modelId` |
| `inference.dedup.hit.count` | Number of inputs served from cache | `modelId` |
| `inference.fallback.count` | Number of times a fallback was triggered | `modelId`, `type` |
| `inference.pruned.count` | Number of candidates skipped by lazy predicates | `modelId` |

## Execution Tracing

Every `InferenceResult` contains an `ExecutionTrace`—a sequence of timestamped events that occurred during the request.

### Example Trace Events
- `EXECUTION_START`: Initial request entry.
- `DEDUP_HIT`: Input was found in the per-request deduplication cache.
- `FALLBACK_TRIGGERED`: A model call failed or timed out, and a fallback was applied.
- `PRUNING_APPLIED`: Candidates were removed by a lazy predicate.

### Debugging Latency
The trace allows you to see the exact timeline of which models ran in parallel and where the wall-clock time was spent.
