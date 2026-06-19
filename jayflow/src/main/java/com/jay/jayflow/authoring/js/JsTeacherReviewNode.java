package com.jay.jayflow.authoring.js;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jay.jayflow.ir.spec.TeacherReviewSpec;

/** Equivalent to Rust's JsTeacherReviewNode — wraps TeacherReviewSpec directly under "teacher_review" key. */
public class JsTeacherReviewNode {
    @JsonProperty("teacher_review") public TeacherReviewSpec teacherReview;
}
