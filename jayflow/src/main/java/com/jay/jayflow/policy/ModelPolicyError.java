package com.jay.jayflow.policy;

import java.util.List;

/** Model policy resolution errors, thrown as RuntimeException wrappers. */
public sealed interface ModelPolicyError
        permits ModelPolicyError.MissingPolicy, ModelPolicyError.MissingModel,
               ModelPolicyError.MissingFallbackProvider, ModelPolicyError.NoCapableModel {

    record MissingPolicy(ModelRole role) implements ModelPolicyError {}
    record MissingModel() implements ModelPolicyError {}
    record MissingFallbackProvider(String model) implements ModelPolicyError {}
    record NoCapableModel(ModelRole role, List<String> rejected) implements ModelPolicyError {}

    default void panic() { throw new RuntimeException(this.toString()); }
}
