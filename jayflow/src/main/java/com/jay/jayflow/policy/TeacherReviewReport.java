package com.jay.jayflow.policy;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import com.jay.jayflow.execution.WorkflowExecution;
import com.jay.jayflow.ir.WorkflowNode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TeacherReviewReport(
        @JsonProperty("review_node_id") String reviewNodeId,
        @JsonInclude(NON_EMPTY) List<TeacherCandidate> candidates) {

    public static TeacherReviewReport fromExecution(WorkflowNode.TeacherReview review,
                                                     WorkflowExecution execution) {
        return new TeacherReviewReport(review.id(),
                TeacherCandidates.fromExecution(review, execution));
    }
}
