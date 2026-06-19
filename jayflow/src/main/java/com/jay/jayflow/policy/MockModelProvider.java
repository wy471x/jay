package com.jay.jayflow.policy;

import com.jay.jayflow.execution.WorkflowUsage;

/** Mock model provider for testing. Equivalent to Rust's MockModelProvider. */
public class MockModelProvider implements ModelProvider {
    private final String provider, model, response;
    private final ModelCapabilities capabilities;

    public MockModelProvider(String provider, String model, ModelCapabilities capabilities, String response) {
        this.provider = provider; this.model = model; this.capabilities = capabilities; this.response = response;
    }
    @Override public String provider() { return provider; }
    @Override public String model() { return model; }
    @Override public ModelCapabilities capabilities() { return capabilities; }
    @Override public CompletionResponse complete(CompletionRequest req) throws ModelProviderError {
        return new CompletionResponse(response, new WorkflowUsage());
    }
}
