package com.jay.tools;

/** Approval requirement for a tool. Equivalent to Rust's ApprovalRequirement. */
public enum ApprovalRequirement {
    AUTO,     // Never needs approval
    SUGGEST,  // Suggest but allow skip
    REQUIRED  // Always require explicit approval
}
