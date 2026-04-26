# ML Routing Spring Boot Starter

This module provides seamless auto-configuration for the ML Inference Routing SDK in Spring Boot 3+ applications.

## 🚀 Quick Start

1. **Add the dependency:**
```xml
<dependency>
    <groupId>io.github.shivam61</groupId>
    <artifactId>ml-routing-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

2. **Configure your models:**
Create a `model-registry.yaml` in your `src/main/resources`.

3. **Use the InferenceExecutor:**
The executor is automatically available in your Spring context.

```java
@Service
public class RankingService {
    @Autowired
    private InferenceExecutor executor;

    public void rank(List<Candidate> candidates) {
        // ... build context and plan ...
        executor.execute(plan, context);
    }
}
```

## 🛠️ Customization
You can override the default beans by defining your own `ModelClient`, `MetricsRecorder`, or `ModelRegistry` beans in your configuration.
