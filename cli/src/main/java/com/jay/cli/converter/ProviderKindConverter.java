package com.jay.cli.converter;

import com.jay.agent.ProviderKind;
import picocli.CommandLine.ITypeConverter;

/** picocli type converter for ProviderKind enum. */
public class ProviderKindConverter implements ITypeConverter<ProviderKind> {
    @Override
    public ProviderKind convert(String value) {
        ProviderKind result = ProviderKind.parse(value);
        if (result == null) {
            throw new IllegalArgumentException(
                    "Unknown provider: " + value + ". Use 'jay model list' to see available providers.");
        }
        return result;
    }
}
