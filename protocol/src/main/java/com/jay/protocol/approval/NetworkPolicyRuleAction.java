package com.jay.protocol.approval;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum NetworkPolicyRuleAction {
    @JsonProperty("allow") ALLOW,
    @JsonProperty("deny") DENY
}
