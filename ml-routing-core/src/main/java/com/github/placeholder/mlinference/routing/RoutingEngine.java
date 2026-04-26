package com.github.placeholder.mlinference.routing;

import com.github.placeholder.mlinference.domain.RequestContext;
import java.util.*;
import java.util.stream.Collectors;

public class RoutingEngine {
    private final List<RoutingRule> rules;

    public RoutingEngine(List<RoutingRule> rules) {
        this.rules = rules.stream()
            .filter(RoutingRule::enabled)
            .sorted(Comparator.comparingInt(RoutingRule::priority).reversed())
            .toList();
    }

    public Set<String> route(RequestContext context) {
        for (RoutingRule rule : rules) {
            if (matches(rule, context)) {
                return rule.selectedModels();
            }
        }
        return Collections.emptySet();
    }

    private boolean matches(RoutingRule rule, RequestContext context) {
        RoutingRule.RuleCondition condition = rule.condition();
        
        if (condition.requestType() != null && !condition.requestType().equals(context.requestType())) {
            return false;
        }

        if (condition.attributeMatchers() != null) {
            for (Map.Entry<String, Object> entry : condition.attributeMatchers().entrySet()) {
                Object attrValue = context.attributes().get(entry.getKey());
                if (!Objects.equals(entry.getValue(), attrValue)) {
                    return false;
                }
            }
        }

        return true;
    }
}
