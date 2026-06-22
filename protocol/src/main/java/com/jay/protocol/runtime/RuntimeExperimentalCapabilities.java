package com.jay.protocol.runtime;

public record RuntimeExperimentalCapabilities(boolean environments) {

    public RuntimeExperimentalCapabilities() { this(false); }

}

// ---- DynamicToolSpec ----
