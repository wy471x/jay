package com.jay.protocol.approval;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

public record NetworkPolicyAmendment(String host, NetworkPolicyRuleAction action) {}
