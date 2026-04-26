# Contributing to ML Inference Routing SDK

We welcome contributions! To maintain the quality and architectural integrity of the system, please follow these guidelines.

## Development Setup
1. Use **Java 21**. 
2. Make sure you can build the project: `mvn clean test`
3. If running tests in the Vector module, Maven is already configured to pass `--add-modules jdk.incubator.vector`. Ensure your IDE is also configured to pass this flag to the compiler and test runner.

## Adding Features

**1. No Heavy Frameworks**
Please do not introduce Spring, Guice, or other heavy IoC containers into `ml-routing-core`. The core SDK must remain lightweight so it can be embedded anywhere.

**2. Immutability**
New domain models should be Java `record`s. If you must use a class, ensure all fields are `final` and collections are wrapped in `Collections.unmodifiableList()` or `List.copyOf()`.

**3. Error Handling**
Never silently swallow errors. If a model fails to execute, the DAG Executor handles fallbacks based on the `ModelDefinition`. If you write a new `ModelClient`, bubble up the timeout or failure so the executor can track it in the `ExecutionTrace`.

## Pull Request Process
1. Write tests for your changes.
2. Run `mvn clean test` and verify everything passes.
3. If you introduce performance optimizations, consider adding an JMH benchmark to `ml-routing-benchmarks` to prove the speedup.
