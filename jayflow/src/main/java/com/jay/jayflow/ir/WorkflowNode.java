package com.jay.jayflow.ir;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.jay.jayflow.ir.spec.BranchSpec;
import com.jay.jayflow.ir.spec.CondSpec;
import com.jay.jayflow.ir.spec.ExpandSpec;
import com.jay.jayflow.ir.spec.LeafSpec;
import com.jay.jayflow.ir.spec.LoopUntilSpec;
import com.jay.jayflow.ir.spec.ReduceSpec;
import com.jay.jayflow.ir.spec.SequenceSpec;
import com.jay.jayflow.ir.spec.TeacherReviewSpec;
import java.util.List;

/** 8-variant workflow node sealed interface wrapping separate Spec records.
 *  Delegation methods on each variant provide direct field access for compatibility. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = WorkflowNode.BranchSet.class, name = "branch_set"),
    @JsonSubTypes.Type(value = WorkflowNode.Leaf.class, name = "leaf"),
    @JsonSubTypes.Type(value = WorkflowNode.Sequence.class, name = "sequence"),
    @JsonSubTypes.Type(value = WorkflowNode.Reduce.class, name = "reduce"),
    @JsonSubTypes.Type(value = WorkflowNode.TeacherReview.class, name = "teacher_review"),
    @JsonSubTypes.Type(value = WorkflowNode.LoopUntil.class, name = "loop_until"),
    @JsonSubTypes.Type(value = WorkflowNode.Cond.class, name = "cond"),
    @JsonSubTypes.Type(value = WorkflowNode.Expand.class, name = "expand"),
})
public sealed interface WorkflowNode {

    record BranchSet(BranchSpec spec) implements WorkflowNode {
        public String id() { return spec.id(); }
        public String description() { return spec.description(); }
        public boolean parallel() { return spec.parallel(); }
        public WorkflowConfig.BudgetSpec budget() { return spec.budget(); }
        public WorkflowConfig.PermissionSpec permissions() { return spec.permissions(); }
        public WorkflowConfig.ModelPolicy modelPolicy() { return spec.modelPolicy(); }
        public List<WorkflowNode> children() { return spec.children(); }
    }

    record Leaf(LeafSpec spec) implements WorkflowNode {
        public String id() { return spec.id(); }
        public String prompt() { return spec.prompt(); }
        public AgentType agentType() { return spec.agentType(); }
        public TaskMode mode() { return spec.mode(); }
        public IsolationMode isolation() { return spec.isolation(); }
        @JsonProperty("file_scope") public List<String> fileScope() { return spec.fileScope(); }
        @JsonProperty("depends_on_results") public List<String> dependsOnResults() { return spec.dependsOnResults(); }
        public WorkflowConfig.BudgetSpec budget() { return spec.budget(); }
        public WorkflowConfig.PermissionSpec permissions() { return spec.permissions(); }
        public WorkflowConfig.ModelPolicy modelPolicy() { return spec.modelPolicy(); }
    }

    record Sequence(SequenceSpec spec) implements WorkflowNode {
        public String id() { return spec.id(); }
        public List<WorkflowNode> children() { return spec.children(); }
    }

    record Reduce(ReduceSpec spec) implements WorkflowNode {
        public String id() { return spec.id(); }
        public List<String> inputs() { return spec.inputs(); }
        public String prompt() { return spec.prompt(); }
        public WorkflowConfig.ModelPolicy modelPolicy() { return spec.modelPolicy(); }
    }

    record TeacherReview(TeacherReviewSpec spec) implements WorkflowNode {
        public String id() { return spec.id(); }
        public List<String> candidates() { return spec.candidates(); }
        public WorkflowConfig.PromotionPolicy promotionPolicy() { return spec.promotionPolicy(); }
    }

    record LoopUntil(LoopUntilSpec spec) implements WorkflowNode {
        public String id() { return spec.id(); }
        public String condition() { return spec.condition(); }
        @JsonProperty("max_iterations") public Integer maxIterations() { return spec.maxIterations(); }
        public List<WorkflowNode> children() { return spec.children(); }
    }

    record Cond(CondSpec spec) implements WorkflowNode {
        public String id() { return spec.id(); }
        public String condition() { return spec.condition(); }
        @JsonProperty("then_nodes") public List<WorkflowNode> thenNodes() { return spec.thenNodes(); }
        @JsonProperty("else_nodes") public List<WorkflowNode> elseNodes() { return spec.elseNodes(); }
    }

    record Expand(ExpandSpec spec) implements WorkflowNode {
        public String id() { return spec.id(); }
        public String source() { return spec.source(); }
        @JsonProperty("max_children") public Integer maxChildren() { return spec.maxChildren(); }
        public WorkflowNode template() { return spec.template(); }
    }
}
