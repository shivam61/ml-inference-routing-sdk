# Configuration Reference

The SDK uses YAML/JSON for model and routing configuration.

## Model Definition (`model-registry.yaml`)

```yaml
models:
  - modelId: string          # Unique identifier for the model
    modelFamily: string      # Logical grouping (e.g., ranking, risk)
    version: string          # Model version tag
    backendType: enum        # REMOTE, TRITON, TF_SERVING, LOCAL_ONNX, LOCAL_VECTOR, IN_MEMORY
    timeoutMs: long          # Execution timeout
    maxBatchSize: int        # Max candidates per batch call
    dependencies: [string]   # List of upstream modelIds
    lazyPredicate:           # Optional: Pruning logic
      type: TOP_N
      upstreamModel: string
      params: { n: 10 }
    fallbackStrategy:        # Optional: Failure handling
      type: enum             # CONSTANT_SCORE, DEFAULT_OUTPUT, SKIP_MODEL, FAIL_FAST
      value: double          # Used for CONSTANT_SCORE
    tags: { key: value }     # Metadata
```

## Routing Rules (`routing-rules.yaml`)

```yaml
rules:
  - ruleId: string           # Unique rule name
    priority: int            # Higher numbers win
    enabled: boolean         # Feature flag
    condition:               # Matching logic
      requestType: string    # Matches RequestContext.requestType
      attributeMatchers:     # Key-value matches for context attributes
        market: "US"
    selectedModels: [string] # Models to trigger (automatically pulls dependencies)
```
