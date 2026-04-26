# AI Agent Integration Guide

The ML Inference Routing SDK is designed with **AI-driven development** in mind. Whether you are using Cursor, Windsurf, or a custom autonomous agent, this guide explains how to leverage the SDK's declarative and traceable nature.

## 1. Context-Driven Development
The repository includes a `.cursorrules` file. This file provides explicit instructions to AI agents about:
- **Java 21 Standards:** Using Records and Virtual Threads.
- **Architectural Constraints:** No Spring/Guice in core, strict immutability.
- **Generic Terminology:** Ensuring the agent doesn't introduce domain-specific jargon.

## 2. Declarative Configuration
Most integration tasks involve updating YAML files rather than writing complex orchestration code. This significantly reduces the "hallucination surface" for AI agents.

### Adding a new Model
To add a model, an AI agent only needs to modify `model-registry.yaml`. The SDK handles the wiring.
```yaml
# AI Agent can easily append this
- modelId: sentiment_analysis
  backendType: LOCAL_ONNX
  timeoutMs: 20
```

### Modifying Routing
Rules are also declarative. An AI can quickly adjust priorities or conditions without touching Java code.

## 3. High-Signal Traceability for Debugging
When an agent is tasked with "fixing a latency issue" or "debugging a missing score," it can look at the `ExecutionTrace`.

```java
InferenceResult result = executor.execute(plan, context).get();
// AI Agent can print and analyze the trace:
result.executionTrace().events().forEach(System.out::println);
```

The trace will explicitly show:
- `CIRCUIT_BREAKER_OPEN`: Why a model was skipped.
- `DEDUP_HIT`: Where computation was saved.
- `PRUNING_APPLIED`: Why certain candidates didn't reach the final stage.

## 4. Simplified Integration Pattern
For an AI agent to integrate this SDK into a new service, it only needs to follow this boilerplate:

1.  **Initialize Registry:** `ModelRegistry registry = new ModelRegistry();`
2.  **Load Configs:** Use `ConfigurationLoader`.
3.  **Implement Client:** Create a simple `ModelClient` wrapper for your specific API.
4.  **Execute:** Use the `InferenceExecutor` (which automatically manages its own thread pool via Project Loom).

## 5. Testing with AI
The SDK's use of **Fake/Simulated Clients** makes it very easy for AI agents to write high-quality unit and integration tests without needing a real ML model serving environment.
