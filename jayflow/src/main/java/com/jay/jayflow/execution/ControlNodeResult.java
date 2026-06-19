package com.jay.jayflow.execution;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import com.jay.jayflow.ir.ControlNodeKind;
import com.jay.jayflow.ir.WorkflowRunStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ControlNodeResult(
        @JsonProperty("node_id") String nodeId,
        ControlNodeKind kind,
        WorkflowRunStatus status,
        @JsonInclude(NON_EMPTY) @JsonProperty("selected_children") List<String> selectedChildren,
        String summary) {}
