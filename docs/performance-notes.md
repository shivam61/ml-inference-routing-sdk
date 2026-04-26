# Performance Notes

Achieving sub-50ms p99 latency for complex model DAGs requires careful tuning of the following parameters.

## Batching Tradeoffs
Larger batch sizes (`maxBatchSize`) improve throughput and reduce total network calls but can increase the latency of an individual call as the remote service takes longer to process the batch.
- **Recommendation:** Start with a batch size of 32 for remote models and 64-128 for local models.

## The Cost of Deduplication
The SDK performs deduplication within the scope of a single request. 
- **Memory:** Deducing requires hashing inputs. For very large feature vectors, this adds a small CPU overhead.
- **Benefit:** If your data contains high repetition (e.g., scoring the same item across different user contexts), dedup can reduce model load by 50%+.

## Why No Retries?
In online inference, a 20ms timeout usually means the service is saturated. Retrying immediately compounds the congestion and exhausts the caller's global deadline.
- **Better Approach:** Use `CONSTANT_SCORE` fallbacks to return a safe, "neutral" result and maintain system stability.

## Parallel Stage Execution
The `ExecutionPlanner` automatically identifies independent models. Ensure your `ExecutorService` (worker pool) is sized correctly.
- **Sizing:** `threads = (number_of_parallel_models * concurrent_requests)`.
- **Note:** Avoid using an unbounded thread pool to prevent JVM crashes under load spikes.
