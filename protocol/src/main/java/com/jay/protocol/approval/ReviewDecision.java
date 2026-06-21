package com.jay.protocol.approval;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ReviewDecision.Approved.class, name = "approved"),
    @JsonSubTypes.Type(value = ReviewDecision.ApprovedExecpolicyAmendment.class, name = "approved_execpolicy_amendment"),
    @JsonSubTypes.Type(value = ReviewDecision.ApprovedForSession.class, name = "approved_for_session"),
    @JsonSubTypes.Type(value = ReviewDecision.NetworkPolicyAmendment.class, name = "network_policy_amendment"),
    @JsonSubTypes.Type(value = ReviewDecision.Denied.class, name = "denied"),
    @JsonSubTypes.Type(value = ReviewDecision.Abort.class, name = "abort"),
})
public sealed interface ReviewDecision {
    record Approved() implements ReviewDecision {}
    record ApprovedExecpolicyAmendment() implements ReviewDecision {}
    record ApprovedForSession() implements ReviewDecision {}
    record NetworkPolicyAmendment(String host, NetworkPolicyRuleAction action) implements ReviewDecision {}
    record Denied() implements ReviewDecision {}
    record Abort() implements ReviewDecision {}
}
