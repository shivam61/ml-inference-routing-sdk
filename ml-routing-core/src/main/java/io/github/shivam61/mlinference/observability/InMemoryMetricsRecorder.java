package io.github.shivam61.mlinference.observability;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * A simple in-memory metrics implementation for testing and CLI demos.
 */
public class InMemoryMetricsRecorder implements MetricsRecorder {
    private final Map<String, LongAdder> counters = new ConcurrentHashMap<>();

    @Override
    public void recordCounter(String name, long count, Map<String, String> tags) {
        String key = name + tags.toString();
        counters.computeIfAbsent(key, k -> new LongAdder()).add(count);
    }

    @Override
    public void recordTimer(String name, long durationMs, Map<String, String> tags) {
        // Simple console print or just ignore for in-memory
    }

    public long getCount(String name, Map<String, String> tags) {
        String key = name + tags.toString();
        return counters.getOrDefault(key, new LongAdder()).sum();
    }

    public void printSummary() {
        System.out.println("\n--- Metrics Summary ---");
        counters.forEach((k, v) -> System.out.println(k + ": " + v.sum()));
    }
}
