package com.github.placeholder.mlinference.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.placeholder.mlinference.domain.ModelDefinition;
import com.github.placeholder.mlinference.routing.RoutingRule;
import java.io.InputStream;
import java.util.List;

public class ConfigurationLoader {
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory())
        .registerModule(new Jdk8Module())
        .registerModule(new JavaTimeModule());

    public record ModelRegistryConfig(List<ModelDefinition> models) {}
    public record RoutingRulesConfig(List<RoutingRule> rules) {}

    public static List<ModelDefinition> loadModels(InputStream is) throws Exception {
        return YAML_MAPPER.readValue(is, ModelRegistryConfig.class).models();
    }

    public static List<RoutingRule> loadRules(InputStream is) throws Exception {
        return YAML_MAPPER.readValue(is, RoutingRulesConfig.class).rules();
    }
}
