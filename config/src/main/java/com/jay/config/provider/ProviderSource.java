package com.jay.config.provider;

public sealed interface ProviderSource {
    record Cli() implements ProviderSource { }

    record Env(String varName) implements ProviderSource { }

    record Config() implements ProviderSource { }
}
