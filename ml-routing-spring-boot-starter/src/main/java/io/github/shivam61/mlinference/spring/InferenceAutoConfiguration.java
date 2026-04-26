package io.github.shivam61.mlinference.spring;

import io.github.shivam61.mlinference.client.CompositeModelClient;
import io.github.shivam61.mlinference.client.ModelClient;
import io.github.shivam61.mlinference.executor.InferenceExecutor;
import io.github.shivam61.mlinference.observability.MetricsRecorder;
import io.github.shivam61.mlinference.registry.ModelRegistry;
import io.github.shivam61.mlinference.routing.RoutingEngine;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.util.List;

@AutoConfiguration
public class InferenceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ModelRegistry modelRegistry() {
        return new ModelRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public RoutingEngine routingEngine() {
        return new RoutingEngine(List.of());
    }

    @Bean
    @ConditionalOnMissingBean
    public MetricsRecorder metricsRecorder() {
        return MetricsRecorder.NOOP;
    }

    @Bean
    @ConditionalOnMissingBean
    public ModelClient modelClient(List<ModelClient> clients) {
        return new CompositeModelClient(clients);
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public InferenceExecutor inferenceExecutor(ModelClient client, MetricsRecorder recorder) {
        return new InferenceExecutor(client, recorder);
    }
}
