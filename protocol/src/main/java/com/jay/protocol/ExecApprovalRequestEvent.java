package com.jay.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
public record ExecApprovalRequestEvent(
        @JsonProperty("call_id") String callId,
        @JsonProperty("approval_id") String approvalId,
        @JsonProperty("turn_id") String turnId,
        String command, String cwd, String reason,
        @JsonProperty("matched_rule") String matchedRule,
        @JsonProperty("network_approval_context") NetworkApprovalContext networkApprovalContext,
        @JsonProperty("proposed_execpolicy_amendment") List<String> proposedExecpolicyAmendment,
        @JsonProperty("proposed_network_policy_amendments") List<NetworkPolicyAmendment> proposedNetworkPolicyAmendments,
        @JsonProperty("additional_permissions") List<String> additionalPermissions,
        @JsonProperty("available_decisions") List<ReviewDecision> availableDecisions) {
    public ExecApprovalRequestEvent {
        if (proposedExecpolicyAmendment == null) proposedExecpolicyAmendment = List.of();
        if (proposedNetworkPolicyAmendments == null) proposedNetworkPolicyAmendments = List.of();
        if (additionalPermissions == null) additionalPermissions = List.of();
        if (availableDecisions == null) availableDecisions = List.of();
    }
}
