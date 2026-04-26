package io.github.shivam61.mlinference.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Adapter for Micrometer metrics collection.
 */
public class MicrometerMetricsRecorder implements MetricsRecorder {
    private final MeterRegistry registry;

    public MicrometerMetricsRecorder(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void recordCounter(String name, long count, Map<String, String> tags) {
        registry.counter(name, toMicrometerTags(tags)).increment(count);
    }

    @Override
    public void recordTimer(String name, long durationMs, Map<String, String> tags) {
        Timer.builder(name)
            .tags(toMicrometerTags(tags))
            .register(registry)
            .record(durationMs, TimeUnit.MILLISECONDS);
    }

    private Iterable<Tag> toMicrometerTags(Map<String, String> tags) {
        return tags.entrySet().stream()
            .map(e -> Tag.of(e.getKey(), e.getValue()))
            .collect(Collectors.toList());
    }
}
