package com.jay.execpolicy;

import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight security policy engine.
 * Evaluates tool invocation requests against a configurable rule chain.
 * For the ~1,400-line scope, a simple hand-written matcher suffices —
 * no need for a heavyweight rules engine like Drools.
 */
public class PolicyEngine {

    private final List<PolicyRule> rules = new ArrayList<>();

    public void addRule(PolicyRule rule) {
        rules.add(rule);
    }

    public PolicyResult evaluate(String toolName, String sessionId, String userId) {
        for (var rule : rules) {
            var result = rule.evaluate(toolName, sessionId, userId);
            if (result != PolicyResult.ALLOW) {
                return result;
            }
        }
        return PolicyResult.ALLOW;
    }

    @FunctionalInterface
    public interface PolicyRule {
        PolicyResult evaluate(String toolName, String sessionId, String userId);
    }

    public enum PolicyResult {
        ALLOW,
        DENY,
        REQUIRE_APPROVAL
    }
}
