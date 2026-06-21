package com.jay.protocol.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.jay.protocol.approval.ExecApprovalRequestEvent;
import com.jay.protocol.mcp.McpStartupCompleteEvent;
import com.jay.protocol.mcp.McpStartupUpdateEvent;
import java.util.List;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "event")
@JsonSubTypes({
    @JsonSubTypes.Type(value = EventFrame.ResponseStart.class, name = "response_start"),
    @JsonSubTypes.Type(value = EventFrame.ResponseDelta.class, name = "response_delta"),
    @JsonSubTypes.Type(value = EventFrame.ResponseEnd.class, name = "response_end"),
    @JsonSubTypes.Type(value = EventFrame.ToolCallStart.class, name = "tool_call_start"),
    @JsonSubTypes.Type(value = EventFrame.ToolCallResult.class, name = "tool_call_result"),
    @JsonSubTypes.Type(value = EventFrame.McpStartupUpdate.class, name = "mcp_startup_update"),
    @JsonSubTypes.Type(value = EventFrame.McpStartupComplete.class, name = "mcp_startup_complete"),
    @JsonSubTypes.Type(value = EventFrame.McpToolCallBegin.class, name = "mcp_tool_call_begin"),
    @JsonSubTypes.Type(value = EventFrame.McpToolCallEnd.class, name = "mcp_tool_call_end"),
    @JsonSubTypes.Type(value = EventFrame.ExecApprovalRequest.class, name = "exec_approval_request"),
    @JsonSubTypes.Type(value = EventFrame.ApplyPatchApprovalRequest.class, name = "apply_patch_approval_request"),
    @JsonSubTypes.Type(value = EventFrame.ElicitationRequest.class, name = "elicitation_request"),
    @JsonSubTypes.Type(value = EventFrame.ExecCommandBegin.class, name = "exec_command_begin"),
    @JsonSubTypes.Type(value = EventFrame.ExecCommandOutputDelta.class, name = "exec_command_output_delta"),
    @JsonSubTypes.Type(value = EventFrame.ExecCommandEnd.class, name = "exec_command_end"),
    @JsonSubTypes.Type(value = EventFrame.PatchApplyBegin.class, name = "patch_apply_begin"),
    @JsonSubTypes.Type(value = EventFrame.PatchApplyEnd.class, name = "patch_apply_end"),
    @JsonSubTypes.Type(value = EventFrame.TurnStarted.class, name = "turn_started"),
    @JsonSubTypes.Type(value = EventFrame.TurnComplete.class, name = "turn_complete"),
    @JsonSubTypes.Type(value = EventFrame.TurnAborted.class, name = "turn_aborted"),
    @JsonSubTypes.Type(value = EventFrame.ThreadGoalUpdated.class, name = "thread_goal_updated"),
    @JsonSubTypes.Type(value = EventFrame.ThreadGoalCleared.class, name = "thread_goal_cleared"),
    @JsonSubTypes.Type(value = EventFrame.Error.class, name = "error"),
})
public sealed interface EventFrame {

    @JsonInclude(NON_NULL)
    record ResponseStart(@JsonProperty("response_id") String responseId) implements EventFrame {}

    @JsonInclude(NON_NULL)
    record ResponseDelta(@JsonProperty("response_id") String responseId, String delta,
            @JsonProperty(defaultValue = "text") ResponseChannel channel) implements EventFrame {
        public ResponseDelta(String responseId, String delta) {
            this(responseId, delta, ResponseChannel.TEXT);
        }
    }

    record ResponseEnd(@JsonProperty("response_id") String responseId) implements EventFrame {}

    record ToolCallStart(@JsonProperty("response_id") String responseId,
            @JsonProperty("tool_name") String toolName, JsonNode arguments) implements EventFrame {}

    record ToolCallResult(@JsonProperty("response_id") String responseId,
            @JsonProperty("tool_name") String toolName, JsonNode output) implements EventFrame {}

    record McpStartupUpdate(McpStartupUpdateEvent update) implements EventFrame {}

    record McpStartupComplete(McpStartupCompleteEvent summary) implements EventFrame {}

    record McpToolCallBegin(@JsonProperty("server_name") String serverName,
            @JsonProperty("tool_name") String toolName) implements EventFrame {}

    record McpToolCallEnd(@JsonProperty("server_name") String serverName,
            @JsonProperty("tool_name") String toolName, boolean ok) implements EventFrame {}

    record ExecApprovalRequest(ExecApprovalRequestEvent request) implements EventFrame {}

    record ApplyPatchApprovalRequest(ExecApprovalRequestEvent request) implements EventFrame {}

    record ElicitationRequest(@JsonProperty("server_name") String serverName,
            @JsonProperty("request_id") String requestId, String prompt) implements EventFrame {}

    record ExecCommandBegin(String command, String cwd) implements EventFrame {}

    record ExecCommandOutputDelta(String command, String delta) implements EventFrame {}

    record ExecCommandEnd(String command, @JsonProperty("exit_code") int exitCode) implements EventFrame {}

    record PatchApplyBegin(String path) implements EventFrame {}

    record PatchApplyEnd(String path, boolean ok) implements EventFrame {}

    record TurnStarted(@JsonProperty("turn_id") String turnId) implements EventFrame {}

    record TurnComplete(@JsonProperty("turn_id") String turnId) implements EventFrame {}

    record TurnAborted(@JsonProperty("turn_id") String turnId, String reason) implements EventFrame {}

    record ThreadGoalUpdated(ThreadGoal goal) implements EventFrame {}

    record ThreadGoalCleared(@JsonProperty("thread_id") String threadId) implements EventFrame {}

    record Error(@JsonProperty("response_id") String responseId, String message) implements EventFrame {}
}
