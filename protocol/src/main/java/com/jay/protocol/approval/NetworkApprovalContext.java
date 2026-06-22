package com.jay.protocol.approval;

import com.fasterxml.jackson.annotation.JsonInclude;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
public record NetworkApprovalContext(String host, String protocol) { }
