package io.github.shivam61.mlinference.client;

import io.github.shivam61.mlinference.domain.BackendType;
import io.github.shivam61.mlinference.domain.ModelDefinition;
import io.github.shivam61.mlinference.domain.ModelInput;
import io.github.shivam61.mlinference.domain.ModelOutput;
import io.github.shivam61.mlinference.domain.RequestContext;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CompositeModelClient implements ModelClient {
    private final List<ModelClient> clients;

    public CompositeModelClient(List<ModelClient> clients) {
        this.clients = clients;
    }

    @Override
    public CompletableFuture<List<ModelOutput>> predict(ModelDefinition model, List<ModelInput> inputs, RequestContext ctx) {
        for (ModelClient client : clients) {
            if (client.supports(model.backendType())) {
                return client.predict(model, inputs, ctx);
            }
        }
        return CompletableFuture.failedFuture(new IllegalStateException("No client found for backend type: " + model.backendType()));
    }

    @Override
    public boolean supports(BackendType backendType) {
        return clients.stream().anyMatch(c -> c.supports(backendType));
    }
}
