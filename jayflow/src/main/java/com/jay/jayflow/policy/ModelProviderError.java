package com.jay.jayflow.policy;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jay.jayflow.execution.WorkflowUsage;
import com.jay.jayflow.ir.WorkflowConfig;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

public class ModelProviderError extends RuntimeException {
    public final String provider, model, reason;
    public ModelProviderError(String provider, String model, String reason) {
        super("model provider " + provider + "/" + model + " failed: " + reason);
        this.provider = provider; this.model = model; this.reason = reason;
    }
}
