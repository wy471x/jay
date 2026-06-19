package com.jay.jayflow.execution;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import java.util.*;
import com.jay.jayflow.ir.WorkflowConfig;
import com.jay.jayflow.ir.WorkflowNode;
import com.jay.jayflow.ir.WorkflowRunStatus;
import com.jay.jayflow.ir.WorkflowSpec;
import com.jay.jayflow.validation.WorkflowNodeValidator;
import com.jay.jayflow.validation.WorkflowPlan;

/**
 * WhaleFlow workflow engine — validates configs, compiles plans, manages execution.
 * Equivalent to Rust's whaleflow crate core.
 */
@Service
public class WhaleFlowEngine {
    private static final ObjectMapper mapper = new ObjectMapper();

    public void validate(WorkflowConfig config) { WorkflowPlan.fromConfig(config); }
    public WorkflowPlan compile(WorkflowConfig config) { return WorkflowPlan.fromConfig(config); }
    public WorkflowSpec parseSpec(String json) throws JsonProcessingException { return mapper.readValue(json, WorkflowSpec.class); }
    public String serializeSpec(WorkflowSpec spec) throws JsonProcessingException { return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(spec); }

    public WorkflowExecution createExecution() { return new WorkflowExecution(); }
    public WorkflowExecution transition(WorkflowExecution exec, WorkflowRunStatus s) { return exec.withStatus(s); }
    public void validateNodes(List<WorkflowNode> nodes) { WorkflowNodeValidator.validate(nodes); }
}
