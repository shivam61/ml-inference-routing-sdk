package io.github.shivam61.mlinference.routing;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record RoutingRule(
    String ruleId,
    int priority,
    RuleCondition condition,
    Set<String> selectedModels,
    boolean enabled,
    Map<String, String> tags
) {
    public record RuleCondition(
        String requestType,
        Map<String, Object> attributeMatchers
    ) {}
}
