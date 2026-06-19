package com.jay.jayflow.policy;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PromotionGateDecision(@JsonProperty("candidate_id") String candidateId,
        TeacherCandidateStatus status, @JsonProperty("score_delta") int scoreDelta,
        @JsonProperty("cost_delta_microusd") long costDeltaMicrousd,
        @JsonInclude(NON_EMPTY) List<String> reasons) {
    public boolean promoted() { return status == TeacherCandidateStatus.PROMOTED; }
}
