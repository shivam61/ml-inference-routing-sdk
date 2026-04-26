package io.github.shivam61.mlinference.executor;

import io.github.shivam61.mlinference.client.ModelClient;
import io.github.shivam61.mlinference.domain.*;
import io.github.shivam61.mlinference.observability.MetricsRecorder;
import io.github.shivam61.mlinference.planner.InferencePlan;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class InferenceExecutor implements AutoCloseable {
    private final ModelClient client;
    private final ExecutorService vThreadExecutor;
    private final ScheduledExecutorService scheduler;
    private final MetricsRecorder metrics;
    private final FeatureHasher hasher = new FeatureHasher.CanonicalHasher();
    private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();

    public InferenceExecutor(ModelClient client, MetricsRecorder metrics) {
        this.client = client;
        this.vThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());
        this.metrics = metrics;
    }

    private CircuitBreaker getCircuitBreaker(String modelId) {
        return circuitBreakers.computeIfAbsent(modelId, id -> new CircuitBreaker(id, 5, java.time.Duration.ofSeconds(10)));
    }

    public CompletableFuture<InferenceResult> execute(InferencePlan plan, RequestContext context) {
        return CompletableFuture.supplyAsync(() -> {
            Instant executionStart = Instant.now();
            ExecutionState state = new ExecutionState(context);

            try {
                for (InferencePlan.ExecutionStage stage : plan.stages()) {
                    executeStageSynchronously(stage, state, context);
                }
            } catch (Exception e) {
                state.addEvent(new ExecutionEvent(Instant.now(), "PIPELINE_ERROR", null, Map.of("error", e.getMessage() != null ? e.getMessage() : "Unknown")));
            }

            long totalLatency = Instant.now().toEpochMilli() - executionStart.toEpochMilli();
            metrics.recordTimer("inference.total.latency", totalLatency, Map.of("requestType", context.requestType()));
            
            ExecutionStatus status = state.hasErrors() ? ExecutionStatus.FAILED : 
                                   (state.hasFallbacks() ? ExecutionStatus.PARTIAL_SUCCESS : ExecutionStatus.SUCCESS);

            ExecutionStats stats = new ExecutionStats(
                plan.stages().size(),
                (int) plan.stages().stream().mapToLong(s -> s.models().size()).sum(),
                state.getBatchSizes(),
                state.getDedupHits(),
                state.getPrunedCount(),
                state.getFallbackEvents().size(),
                state.getTimeoutCount(),
                totalLatency
            );

            return new InferenceResult(
                context.requestId(),
                status,
                state.getOutputsByModel(),
                state.getOutputsByCandidate(),
                state.getFallbackEvents(),
                new ExecutionTrace(state.getEvents()),
                stats,
                totalLatency
            );
        }, vThreadExecutor);
    }

    private void executeStageSynchronously(InferencePlan.ExecutionStage stage, ExecutionState state, RequestContext context) throws InterruptedException, ExecutionException {
        try (var scope = new java.util.concurrent.StructuredTaskScope.ShutdownOnFailure()) {
            for (ModelDefinition model : stage.models()) {
                scope.fork(() -> {
                    executeModelSync(model, state, context);
                    return null;
                });
            }
            scope.join();
            scope.throwIfFailed();
        }
    }

    private void executeModelSync(ModelDefinition model, ExecutionState state, RequestContext context) throws InterruptedException, ExecutionException {
        // 0. Check Circuit Breaker
        CircuitBreaker cb = getCircuitBreaker(model.modelId());
        if (!cb.allowRequest()) {
            state.recordFallback(model.modelId(), null, FallbackType.SKIP_MODEL, "Circuit breaker is OPEN");
            state.addEvent(new ExecutionEvent(Instant.now(), "CIRCUIT_BREAKER_OPEN", model.modelId(), Map.of()));
            metrics.recordCounter("inference.fallback.count", 1, Map.of("modelId", model.modelId(), "type", "CIRCUIT_BREAKER"));
            return;
        }

        // 1. Check Global Deadline
        if (context.isDeadlineExceeded()) {
            state.recordFallback(model.modelId(), null, FallbackType.SKIP_MODEL, "Global deadline exceeded before model start");
            metrics.recordCounter("inference.fallback.count", 1, Map.of("modelId", model.modelId(), "type", "DEADLINE_EXCEEDED"));
            return;
        }

        // 2. Identify candidates to run (Apply Lazy Pruning)
        List<Candidate> candidatesToRun = filterCandidates(model, state, context);
        if (candidatesToRun.isEmpty()) {
            return;
        }

        int originalCount = context.candidates().size();
        if (candidatesToRun.size() < originalCount) {
            int pruned = originalCount - candidatesToRun.size();
            state.incrementPrunedCount(pruned);
            metrics.recordCounter("inference.pruned.count", pruned, Map.of("modelId", model.modelId()));
        }

        // 3. Deduplicate inputs
        List<ModelInput> uniqueInputs = deduplicate(model, candidatesToRun, state);
        if (uniqueInputs.isEmpty()) {
            return;
        }

        // 4. Batching
        List<List<ModelInput>> batches = chunk(uniqueInputs, model.maxBatchSize());
        
        try (var scope = new java.util.concurrent.StructuredTaskScope.ShutdownOnFailure()) {
            for (List<ModelInput> batch : batches) {
                scope.fork(() -> {
                    executeBatchSync(model, batch, state, context, cb);
                    return null;
                });
            }
            scope.join();
            // We don't necessarily want to fail the whole stage if one batch fails (handled by fallbacks)
            // but we could scope.throwIfFailed() if we want strict failure.
        }
    }

    private void executeBatchSync(ModelDefinition model, List<ModelInput> batch, ExecutionState state, RequestContext context, CircuitBreaker cb) {
        long remainingDeadline = context.getRemainingTimeMs();
        long timeout = Math.min(model.timeoutMs(), remainingDeadline);
        
        metrics.recordCounter("inference.model.batch_size", batch.size(), Map.of("modelId", model.modelId()));
        long start = System.currentTimeMillis();
        
        try {
            if (timeout <= 0) {
                throw new TimeoutException("No time remaining for batch execution");
            }

            CompletableFuture<List<ModelOutput>> future = client.predict(model, batch, context);
            List<ModelOutput> outputs = future.get(timeout, TimeUnit.MILLISECONDS);
            
            state.recordOutputs(model.modelId(), outputs, batch);
            cb.recordSuccess();
            metrics.recordCounter("inference.model.call.count", 1, Map.of("modelId", model.modelId(), "status", "SUCCESS"));
        } catch (Exception ex) {
            cb.recordFailure();
            metrics.recordCounter("inference.model.call.count", 1, Map.of("modelId", model.modelId(), "status", "FAILURE"));
            if (ex instanceof TimeoutException || ex.getCause() instanceof TimeoutException) {
                metrics.recordCounter("inference.model.timeout.count", 1, Map.of("modelId", model.modelId()));
            }
            applyFallback(model, batch, state, ex);
        } finally {
            metrics.recordTimer("inference.model.latency", System.currentTimeMillis() - start, Map.of("modelId", model.modelId()));
        }
    }

    private List<ModelInput> deduplicate(ModelDefinition model, List<Candidate> candidates, ExecutionState state) {
        List<ModelInput> toRun = new ArrayList<>();
        for (Candidate c : candidates) {
            String hash = hasher.hash(c.features());
            String key = model.modelId() + ":" + hash;
            Optional<ModelOutput> existing = state.getCachedOutput(key);
            if (existing.isPresent()) {
                state.recordOutput(model.modelId(), new ModelOutput(c.candidateId(), model.modelId(), existing.get().score(), existing.get().metadata()));
                state.addEvent(new ExecutionEvent(Instant.now(), "DEDUP_HIT", model.modelId(), Map.of("candidateId", c.candidateId())));
                state.incrementDedupHits();
                metrics.recordCounter("inference.dedup.hit.count", 1, Map.of("modelId", model.modelId()));
            } else {
                toRun.add(new ModelInput(c.candidateId(), c.features()));
            }
        }
        return toRun;
    }

    @Override
    public void close() {
        vThreadExecutor.shutdown();
        scheduler.shutdown();
    }

    private List<Candidate> filterCandidates(ModelDefinition model, ExecutionState state, RequestContext context) {
        if (model.lazyPredicate() == null) return context.candidates();

        // Simple TOP_N logic as an example
        if ("TOP_N".equals(model.lazyPredicate().type())) {
            String upstream = model.lazyPredicate().upstreamModel();
            Object nParam = model.lazyPredicate().params().getOrDefault("n", 10);
            int n = (nParam instanceof Number num) ? num.intValue() : 10;
            
            List<ModelOutput> upstreamOutputs = state.getOutputsForModel(upstream);
            if (upstreamOutputs.isEmpty()) return Collections.emptyList();

            Set<String> topCandidateIds = upstreamOutputs.stream()
                .sorted(Comparator.comparingDouble(ModelOutput::score).reversed())
                .limit(n)
                .map(ModelOutput::candidateId)
                .collect(Collectors.toSet());

            return context.candidates().stream()
                .filter(c -> topCandidateIds.contains(c.candidateId()))
                .toList();
        }

        return context.candidates();
    }

    private void applyFallback(ModelDefinition model, List<ModelInput> batch, ExecutionState state, Throwable ex) {
        FallbackType type = model.fallbackStrategy() != null ? model.fallbackStrategy().type() : FallbackType.FAIL_FAST;
        String reason = (ex instanceof TimeoutException || ex.getCause() instanceof TimeoutException) ? "Timeout" : ex.getMessage();

        for (ModelInput input : batch) {
            state.recordFallback(model.modelId(), input.candidateId(), type, reason);
            if (type == FallbackType.CONSTANT_SCORE) {
                state.recordOutput(model.modelId(), new ModelOutput(input.candidateId(), model.modelId(), model.fallbackStrategy().value(), Map.of("fallback", true)));
            }
        }
    }

    private <T> List<List<T>> chunk(List<T> list, int size) {
        if (size <= 0) return List.of(list);
        List<List<T>> chunks = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            chunks.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return chunks;
    }

    // Inner class to manage mutable execution state thread-safely
    private static class ExecutionState {
        private final Map<String, List<ModelOutput>> outputsByModel = new ConcurrentHashMap<>();
        private final Map<String, List<ModelOutput>> outputsByCandidate = new ConcurrentHashMap<>();
        private final List<InferenceResult.FallbackEvent> fallbackEvents = Collections.synchronizedList(new ArrayList<>());
        private final List<ExecutionEvent> events = Collections.synchronizedList(new ArrayList<>());
        private final Map<String, ModelOutput> dedupCache = new ConcurrentHashMap<>();
        private final Map<String, Integer> batchSizes = new ConcurrentHashMap<>();
        private final java.util.concurrent.atomic.LongAdder dedupHits = new java.util.concurrent.atomic.LongAdder();
        private final java.util.concurrent.atomic.LongAdder prunedCount = new java.util.concurrent.atomic.LongAdder();
        private final java.util.concurrent.atomic.LongAdder timeoutCount = new java.util.concurrent.atomic.LongAdder();
        private boolean hasErrors = false;

        public ExecutionState(RequestContext ctx) {
            addEvent(new ExecutionEvent(Instant.now(), "EXECUTION_START", null, Map.of("requestId", ctx.requestId())));
        }

        public void recordOutputs(String modelId, List<ModelOutput> outputs, List<ModelInput> originalBatch) {
            batchSizes.put(modelId, originalBatch.size());
            Map<String, ModelInput> inputMap = originalBatch.stream()
                .collect(Collectors.toMap(ModelInput::candidateId, i -> i));
            
            FeatureHasher hasher = new FeatureHasher.CanonicalHasher();

            for (ModelOutput o : outputs) {
                recordOutput(modelId, o);
                
                // Cache for deduplication: modelId + features hash
                ModelInput input = inputMap.get(o.candidateId());
                if (input != null) {
                    String hash = hasher.hash(input.features());
                    String key = modelId + ":" + hash;
                    dedupCache.putIfAbsent(key, o);
                }
            }
        }

        public void recordOutput(String modelId, ModelOutput output) {
            outputsByModel.computeIfAbsent(modelId, k -> Collections.synchronizedList(new ArrayList<>())).add(output);
            outputsByCandidate.computeIfAbsent(output.candidateId(), k -> Collections.synchronizedList(new ArrayList<>())).add(output);
        }

        public void recordFallback(String modelId, String candidateId, FallbackType type, String reason) {
            fallbackEvents.add(new InferenceResult.FallbackEvent(modelId, candidateId, type, reason));
            if ("Timeout".equals(reason)) {
                timeoutCount.increment();
            }
            if (type == FallbackType.FAIL_FAST) {
                hasErrors = true;
            }
        }

        public void incrementDedupHits() { dedupHits.increment(); }
        public void incrementPrunedCount(int count) { prunedCount.add(count); }

        public Optional<ModelOutput> getCachedOutput(String key) {
            return Optional.ofNullable(dedupCache.get(key));
        }

        public List<ModelOutput> getOutputsForModel(String modelId) {
            return outputsByModel.getOrDefault(modelId, List.of());
        }

        public Map<String, List<ModelOutput>> getOutputsByModel() { return outputsByModel; }
        public Map<String, List<ModelOutput>> getOutputsByCandidate() { return outputsByCandidate; }
        public List<InferenceResult.FallbackEvent> getFallbackEvents() { return fallbackEvents; }
        public List<ExecutionEvent> getEvents() { return events; }
        public boolean hasFallbacks() { return !fallbackEvents.isEmpty(); }
        public boolean hasErrors() { return hasErrors; }
        public int getDedupHits() { return dedupHits.intValue(); }
        public int getPrunedCount() { return prunedCount.intValue(); }
        public int getTimeoutCount() { return timeoutCount.intValue(); }
        public Map<String, Integer> getBatchSizes() { return batchSizes; }
    }
}
