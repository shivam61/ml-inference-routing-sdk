package com.github.placeholder.mlinference.observability;

import java.util.Map;

public interface MetricsRecorder {
    void recordCounter(String name, long count, Map<String, String> tags);
    void recordTimer(String name, long durationMs, Map<String, String> tags);
    
    MetricsRecorder NOOP = new MetricsRecorder() {
        @Override public void recordCounter(String name, long count, Map<String, String> tags) {}
        @Override public void recordTimer(String name, long durationMs, Map<String, String> tags) {}
    };
}
