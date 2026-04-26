package io.github.shivam61.mlinference.domain;

import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Detailed execution statistics for a single request.
 */
public record ExecutionStats(
    int stageCount,
    int totalModelsInPlan,
    Map<String, Integer> batchSizes,
    int dedupHits,
    int prunedCount,
    int fallbackCount,
    int timeoutCount,
    long totalLatencyMs
) {
    public String explain() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n📊 Execution Summary (Post-Execution Explain)\n");
        sb.append("==========================================\n");
        sb.append(String.format("Stages: %-5d | Total Models: %-5d | Latency: %dms\n", 
            stageCount, totalModelsInPlan, totalLatencyMs));
        
        sb.append("\nEfficiency Metrics:\n");
        sb.append(String.format("  - Deduplication Hits: %d (redundant computations saved)\n", dedupHits));
        sb.append(String.format("  - Pruned Candidates:  %d (unnecessary work avoided)\n", prunedCount));
        
        if (!batchSizes.isEmpty()) {
            sb.append("\nBatching info:\n");
            batchSizes.forEach((model, size) -> 
                sb.append(String.format("  - %-20s -> Batch Size: %d\n", model, size)));
        }

        if (fallbackCount > 0 || timeoutCount > 0) {
            sb.append("\nResiliency Events:\n");
            sb.append(String.format("  - Timeouts:  %d\n", timeoutCount));
            sb.append(String.format("  - Fallbacks: %d applied\n", fallbackCount));
        }
        
        sb.append("==========================================\n");
        return sb.toString();
    }
}
