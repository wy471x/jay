package com.jay.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

public record NetworkPolicyAmendment(String host, NetworkPolicyRuleAction action) {}

// ---- Approval decision request ----
