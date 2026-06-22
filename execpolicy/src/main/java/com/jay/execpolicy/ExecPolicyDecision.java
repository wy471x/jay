package com.jay.execpolicy;

public record ExecPolicyDecision(
    boolean allow,
    boolean requiresApproval,
    ExecApprovalRequirement requirement,
    String matchedRule
) { }
