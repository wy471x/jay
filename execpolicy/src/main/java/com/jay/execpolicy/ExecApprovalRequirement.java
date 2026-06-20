package com.jay.execpolicy;

import com.jay.protocol.NetworkPolicyAmendment;
import java.util.List;

public sealed interface ExecApprovalRequirement {

    record Skip(
        boolean bypassSandbox,
        ExecPolicyAmendment proposedExecpolicyAmendment
    ) implements ExecApprovalRequirement {}

    record NeedsApproval(
        String reason,
        ExecPolicyAmendment proposedExecpolicyAmendment,
        List<NetworkPolicyAmendment> proposedNetworkPolicyAmendments
    ) implements ExecApprovalRequirement {}

    record Forbidden(String reason) implements ExecApprovalRequirement {}

    default String reason() {
        return switch (this) {
            case Skip s -> "skip";
            case NeedsApproval n -> n.reason();
            case Forbidden f -> f.reason();
        };
    }

    default String phase() {
        return switch (this) {
            case Skip s -> "allowed";
            case NeedsApproval n -> "needs_approval";
            case Forbidden f -> "forbidden";
        };
    }
}
