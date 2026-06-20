package com.jay.hooks;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.jay.protocol.EventFrame;

/**
 * Lifecycle hook event — equivalent to Rust's HookEvent enum with 7 variants.
 * Uses Jackson polymorphic serialization with snake_case type discriminator.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = HookEvent.ResponseStart.class, name = "response_start"),
    @JsonSubTypes.Type(value = HookEvent.ResponseDelta.class, name = "response_delta"),
    @JsonSubTypes.Type(value = HookEvent.ResponseEnd.class, name = "response_end"),
    @JsonSubTypes.Type(value = HookEvent.ToolLifecycle.class, name = "tool_lifecycle"),
    @JsonSubTypes.Type(value = HookEvent.JobLifecycle.class, name = "job_lifecycle"),
    @JsonSubTypes.Type(value = HookEvent.ApprovalLifecycle.class, name = "approval_lifecycle"),
    @JsonSubTypes.Type(value = HookEvent.GenericEventFrame.class, name = "generic_event_frame"),
})
public sealed interface HookEvent {

    /** A new response stream has started. */
    record ResponseStart(String responseId) implements HookEvent {
        public ResponseStart {
            if (responseId == null) responseId = "";
        }
    }

    /** A text chunk arrived for an in-progress response. */
    record ResponseDelta(String responseId, String delta) implements HookEvent {}

    /** A response stream finished. */
    record ResponseEnd(String responseId) implements HookEvent {}

    /** A tool invocation changed phase (start, end, error, etc.). */
    record ToolLifecycle(String responseId, String toolName, String phase, JsonNode payload)
            implements HookEvent {}

    /** A background job changed phase (queued, running, done, etc.). */
    record JobLifecycle(String jobId, String phase, Integer progress, String detail)
            implements HookEvent {}

    /** An approval request changed phase (requested, approved, denied, etc.). */
    record ApprovalLifecycle(String approvalId, String phase, String reason)
            implements HookEvent {}

    /** Catch-all wrapping an arbitrary protocol-level EventFrame. */
    record GenericEventFrame(EventFrame frame) implements HookEvent {}
}
