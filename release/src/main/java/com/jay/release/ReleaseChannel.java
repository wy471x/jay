package com.jay.release;

public enum ReleaseChannel {
    STABLE,
    BETA;

    public static ReleaseChannel fromBetaFlag(boolean beta) {
        return beta ? BETA : STABLE;
    }

    public String label() {
        return this == STABLE ? "stable" : "beta";
    }
}
