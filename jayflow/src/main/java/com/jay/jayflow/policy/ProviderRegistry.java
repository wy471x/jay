package com.jay.jayflow.policy;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.jay.jayflow.ir.WorkflowConfig;

/**
 * Registry of available models with role-based resolution.
 * Equivalent to Rust's ProviderRegistry in model_policy.rs.
 */
public class ProviderRegistry {

    private final Map<String, ProviderModel> models = new LinkedHashMap<>();
    private final Map<ModelRole, WorkflowConfig.ModelPolicy> rolePolicies = new EnumMap<>(ModelRole.class);

    public ProviderRegistry withModel(ProviderModel model) {
        models.put(model.provider() + "/" + model.model(), model);
        return this;
    }

    public ProviderRegistry withRolePolicy(ModelRole role, WorkflowConfig.ModelPolicy policy) {
        rolePolicies.put(role, policy);
        return this;
    }

    public ResolvedModel resolveRole(ModelRole role, WorkflowConfig.ModelPolicy policy,
                                      ModelCapabilities required) {
        var resolvedPolicy = policy != null ? policy : rolePolicies.get(role);
        if (resolvedPolicy == null)
            new ModelPolicyError.MissingPolicy(role).panic();

        var source = policy != null ? ModelSelectionSource.PRIMARY : ModelSelectionSource.ROLE_DEFAULT;
        return resolvePolicy(role, resolvedPolicy, source, required);
    }

    private ResolvedModel resolvePolicy(ModelRole role, WorkflowConfig.ModelPolicy policy,
                                         ModelSelectionSource primarySource, ModelCapabilities required) {
        if (policy.model() == null) new ModelPolicyError.MissingModel().panic();

        var candidates = modelCandidates(policy);
        var rejected = new ArrayList<String>();
        for (int i = 0; i < candidates.size(); i++) {
            var c = candidates.get(i);
            var key = c.provider() + "/" + c.model();
            var model = models.get(key);
            if (model == null) {
                rejected.add(key + ": unknown");
                continue;
            }
            if (model.capabilities().satisfies(required)) {
                return new ResolvedModel(role, model.provider(), model.model(),
                        model.capabilities(), i == 0 ? primarySource : ModelSelectionSource.FALLBACK);
            }
            rejected.add(key + ": missing required capabilities");
        }
        new ModelPolicyError.NoCapableModel(role, rejected).panic();
        throw new IllegalStateException("unreachable"); // .panic() always throws
    }

    private List<ModelCandidate> modelCandidates(WorkflowConfig.ModelPolicy policy) {
        var list = new ArrayList<ModelCandidate>();
        var m = policy.model();
        if (m == null) return list;
        list.add(candidateFromModel(policy.provider(), m));
        for (var fb : policy.fallbackModels() != null ? policy.fallbackModels() : List.<String>of())
            list.add(candidateFromModel(policy.provider(), fb));
        return list;
    }

    private ModelCandidate candidateFromModel(String defaultProvider, String model) {
        if (model.contains("/")) {
            var parts = model.split("/", 2);
            return new ModelCandidate(parts[0], parts[1]);
        }
        if (defaultProvider == null)
            new ModelPolicyError.MissingFallbackProvider(model).panic();
        return new ModelCandidate(defaultProvider, model);
    }

    private record ModelCandidate(String provider, String model) { }
}
