package io.github.shivam61.mlinference.routing;

import io.github.shivam61.mlinference.domain.RequestContext;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class RoutingEngineTest {

    @Test
    void shouldMatchRuleByRequestType() {
        RoutingRule rule = new RoutingRule("r1", 100, new RoutingRule.RuleCondition("SEARCH", Map.of()), Set.of("m1"), true, Map.of());
        RoutingEngine engine = new RoutingEngine(List.of(rule));
        
        RequestContext ctx = new RequestContext("id", "SEARCH", Instant.now(), null, Map.of(), List.of(), Map.of());
        
        assertThat(engine.route(ctx)).containsExactly("m1");
    }

    @Test
    void shouldRespectPriority() {
        RoutingRule low = new RoutingRule("low", 10, new RoutingRule.RuleCondition("SEARCH", Map.of()), Set.of("m_low"), true, Map.of());
        RoutingRule high = new RoutingRule("high", 200, new RoutingRule.RuleCondition("SEARCH", Map.of()), Set.of("m_high"), true, Map.of());
        
        RoutingEngine engine = new RoutingEngine(List.of(low, high));
        RequestContext ctx = new RequestContext("id", "SEARCH", Instant.now(), null, Map.of(), List.of(), Map.of());
        
        assertThat(engine.route(ctx)).containsExactly("m_high");
    }

    @Test
    void shouldIgnoreDisabledRules() {
        RoutingRule disabled = new RoutingRule("r1", 100, new RoutingRule.RuleCondition("SEARCH", Map.of()), Set.of("m1"), false, Map.of());
        RoutingEngine engine = new RoutingEngine(List.of(disabled));
        
        RequestContext ctx = new RequestContext("id", "SEARCH", Instant.now(), null, Map.of(), List.of(), Map.of());
        
        assertThat(engine.route(ctx)).isEmpty();
    }
}
