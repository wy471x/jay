package com.jay.protocol.approval;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = AskForApproval.UnlessTrusted.class, name = "unless_trusted"),
    @JsonSubTypes.Type(value = AskForApproval.OnFailure.class, name = "on_failure"),
    @JsonSubTypes.Type(value = AskForApproval.OnRequest.class, name = "on_request"),
    @JsonSubTypes.Type(value = AskForApproval.Reject.class, name = "reject"),
    @JsonSubTypes.Type(value = AskForApproval.Never.class, name = "never"),
})
public sealed interface AskForApproval {
    record UnlessTrusted() implements AskForApproval {}
    record OnFailure() implements AskForApproval {}
    record OnRequest() implements AskForApproval {}
    record Reject(@JsonProperty("sandbox_approval") boolean sandboxApproval,
                  boolean rules, @JsonProperty("mcp_elicitations") boolean mcpElicitations) implements AskForApproval {}
    record Never() implements AskForApproval {}
}
