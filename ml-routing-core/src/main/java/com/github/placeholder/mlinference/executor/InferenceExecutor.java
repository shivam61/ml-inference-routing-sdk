package com.github.placeholder.mlinference.executor;

import com.github.placeholder.mlinference.client.ModelClient;
import com.github.placeholder.mlinference.domain.*;
import com.github.placeholder.mlinference.observability.MetricsRecorder;
import com.github.placeholder.mlinference.planner.InferencePlan;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class InferenceExecutor implements AutoCloseable {
    private final ModelClient client;
    private final ExecutorService vThreadExecutor;
    private final ScheduledExecutorService scheduler;
    private final MetricsRecorder metrics;
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
                // Unexpected error in execution pipeline
            }

            long totalLatency = Instant.now().toEpochMilli() - executionStart.toEpochMilli();
            ExecutionStatus status = state.hasFallbacks() ? ExecutionStatus.PARTIAL_SUCCESS : ExecutionStatus.SUCCESS;

            return new InferenceResult(
                context.requestId(),
                status,
                state.getOutputsByModel(),
                state.getOutputsByCandidate(),
                state.getFallbackEvents(),
                new ExecutionTrace(state.getEvents()),
                totalLatency
            );
        }, vThreadExecutor);
    }

    private void executeStageSynchronously(InferencePlan.ExecutionStage stage, ExecutionState state, RequestContext context) throws InterruptedException, ExecutionException {
        List<Future<Void>> futures = new ArrayList<>();
        for (ModelDefinition model : stage.models()) {
            futures.add(vThreadExecutor.submit(() -> {
                executeModelSync(model, state, context);
                return null;
            }));
        }
        for (Future<Void> f : futures) {
            f.get(); // Wait for all models in this stage to complete
        }
    }

    private void executeModelSync(ModelDefinition model, ExecutionState state, RequestContext context) {
        // 0. Check Circuit Breaker
        CircuitBreaker cb = getCircuitBreaker(model.modelId());
        if (!cb.allowRequest()) {
            state.recordFallback(model.modelId(), null, FallbackType.SKIP_MODEL, "Circuit breaker is OPEN");
            state.addEvent(new ExecutionEvent(Instant.now(), "CIRCUIT_BREAKER_OPEN", model.modelId(), Map.of()));
            return;
        }

        // 1. Check Global Deadline
        if (context.isDeadlineExceeded()) {
            state.recordFallback(model.modelId(), null, FallbackType.SKIP_MODEL, "Global deadline exceeded before model start");
            return;
        }

        // 2. Identify candidates to run (Apply Lazy Pruning)
        List<Candidate> candidatesToRun = filterCandidates(model, state, context);
        if (candidatesToRun.isEmpty()) {
            return;
        }

        // 3. Deduplicate inputs
        List<ModelInput> uniqueInputs = deduplicate(model, candidatesToRun, state);
        if (uniqueInputs.isEmpty()) {
            return;
        }

        // 4. Batching
        List<List<ModelInput>> batches = chunk(uniqueInputs, model.maxBatchSize());
        
        List<Future<Void>> batchFutures = new ArrayList<>();
        for (List<ModelInput> batch : batches) {
            batchFutures.add(vThreadExecutor.submit(() -> {
                executeBatchSync(model, batch, state, context, cb);
                return null;
            }));
        }

        for (Future<Void> f : batchFutures) {
            try {
                f.get();
            } catch (Exception e) {
                // Handled within executeBatchSync
            }
        }
    }

    private void executeBatchSync(ModelDefinition model, List<ModelInput> batch, ExecutionState state, RequestContext context, CircuitBreaker cb) {
        long timeout = Math.min(model.timeoutMs(), context.getRemainingTimeMs());
        
        try {
            CompletableFuture<List<ModelOutput>> future = client.predict(model, batch, context);
            List<ModelOutput> outputs;
            if (timeout > 0) {
                outputs = future.get(timeout, TimeUnit.MILLISECONDS);
            } else {
                outputs = future.get();
            }
            state.recordOutputs(model.modelId(), outputs, batch);
            cb.recordSuccess();
        } catch (Exception ex) {
            cb.recordFailure();
            applyFallback(model, batch, state, ex);
        }
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
            int n = (int) model.lazyPredicate().params().getOrDefault("n", 10);
            
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

    private List<ModelInput> deduplicate(ModelDefinition model, List<Candidate> candidates, ExecutionState state) {
        List<ModelInput> toRun = new ArrayList<>();
        for (Candidate c : candidates) {
            String key = model.modelId() + ":" + c.features().hashCode();
            Optional<ModelOutput> existing = state.getCachedOutput(key);
            if (existing.isPresent()) {
                state.recordOutput(model.modelId(), new ModelOutput(c.candidateId(), model.modelId(), existing.get().score(), existing.get().metadata()));
                state.addEvent(new ExecutionEvent(Instant.now(), "DEDUP_HIT", model.modelId(), Map.of("candidateId", c.candidateId())));
            } else {
                toRun.add(new ModelInput(c.candidateId(), c.features()));
            }
        }
        return toRun;
    }

    private void applyFallback(ModelDefinition model, List<ModelInput> batch, ExecutionState state, Throwable ex) {
        FallbackType type = model.fallbackStrategy() != null ? model.fallbackStrategy().type() : FallbackType.FAIL_FAST;
        String reason = ex instanceof TimeoutException ? "Timeout" : ex.getMessage();

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

        public ExecutionState(RequestContext ctx) {
            addEvent(new ExecutionEvent(Instant.now(), "EXECUTION_START", null, Map.of("requestId", ctx.requestId())));
        }

        public void recordOutputs(String modelId, List<ModelOutput> outputs, List<ModelInput> originalBatch) {
            Map<String, ModelInput> inputMap = originalBatch.stream()
                .collect(Collectors.toMap(ModelInput::candidateId, i -> i));

            for (ModelOutput o : outputs) {
                recordOutput(modelId, o);
                
                // Cache for deduplication: modelId + features hash
                ModelInput input = inputMap.get(o.candidateId());
                if (input != null) {
                    String key = modelId + ":" + input.features().hashCode();
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
        }

        public void addEvent(ExecutionEvent event) {
            events.add(event);
        }

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
    }
}
