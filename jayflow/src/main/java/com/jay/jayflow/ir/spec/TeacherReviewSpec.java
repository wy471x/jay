package com.jay.jayflow.ir.spec;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jay.jayflow.ir.WorkflowConfig;
import java.util.List;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/** Equivalent to Rust's TeacherReviewSpec. */
@JsonInclude(NON_NULL)
public record TeacherReviewSpec(String id, List<String> candidates,
        WorkflowConfig.PromotionPolicy promotionPolicy) {}
