package com.jay.jayflow.policy;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.*;
import com.jay.jayflow.ir.WorkflowRunStatus;

/** Branch tournament for selecting best branches. Equivalent to Rust's BranchTournament + ParetoFrontier. */
public record BranchTournament(@JsonProperty("min_score") int minScore) {

    public BranchTournament() { this(60); }

    public Optional<BranchCandidate> select(List<BranchCandidate> candidates) {
        return candidates.stream()
                .filter(c -> c.status() == WorkflowRunStatus.SUCCEEDED && c.score() >= minScore)
                .min(Comparator.comparingLong(BranchCandidate::cost)
                        .thenComparing(Comparator.comparingInt(BranchCandidate::score).reversed()));
    }

    /** Pareto frontier selection — keeps non-dominated candidates. */
    public static List<BranchCandidate> paretoSelect(List<BranchCandidate> candidates, int maxItems) {
        var frontier = candidates.stream()
                .filter(c -> c.status() == WorkflowRunStatus.SUCCEEDED)
                .filter(c -> candidates.stream().noneMatch(other ->
                        other.status() == WorkflowRunStatus.SUCCEEDED
                                && other.score() >= c.score() && other.cost() <= c.cost()
                                && (other.score() > c.score() || other.cost() < c.cost())))
                .sorted(Comparator.comparingInt(BranchCandidate::score).reversed()
                        .thenComparingLong(BranchCandidate::cost))
                .limit(Math.max(maxItems, 1))
                .toList();
        return frontier;
    }
}
