package com.jay.core.thread;

public record NewThread(
    String id,
    String model,
    String modelProvider,
    String cwd,
    String approvalPolicy,
    String sandbox
) { }
