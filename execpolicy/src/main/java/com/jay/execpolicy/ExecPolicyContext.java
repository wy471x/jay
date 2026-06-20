package com.jay.execpolicy;

import com.jay.protocol.AskForApproval;

public record ExecPolicyContext(
    String command,
    String cwd,
    String tool,
    String path,
    AskForApproval askForApproval,
    String sandboxMode
) {
    public ExecPolicyContext {
        if (tool == null) tool = "exec_shell";
    }
}
