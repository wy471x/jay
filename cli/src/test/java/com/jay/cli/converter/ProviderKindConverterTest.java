package com.jay.cli.converter;

import com.jay.agent.ProviderKind;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class ProviderKindConverterTest {

    private final ProviderKindConverter converter = new ProviderKindConverter();

    @Test
    void convertsDeepseek() {
        assertEquals(ProviderKind.DEEPSEEK, converter.convert("deepseek"));
    }

    @Test
    void convertsOpenai() {
        assertEquals(ProviderKind.OPENAI, converter.convert("openai"));
    }

    @Test
    void convertsAnthropic() {
        assertEquals(ProviderKind.ANTHROPIC, converter.convert("anthropic"));
    }

    @Test
    void convertsNvidiaNim() {
        assertEquals(ProviderKind.NVIDIA_NIM, converter.convert("nvidia-nim"));
    }

    @Test
    void convertsWithMixedCase() {
        assertEquals(ProviderKind.DEEPSEEK, converter.convert("DeepSeek"));
        assertEquals(ProviderKind.OPENAI, converter.convert("OpenAI"));
    }

    @Test
    void convertsWithWhitespace() {
        assertEquals(ProviderKind.OPENAI, converter.convert("  openai  "));
    }

    @Test
    void unknownProviderThrows() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> converter.convert("nonexistent-provider-xyz"));
        assertTrue(ex.getMessage().contains("nonexistent-provider-xyz"));
        assertTrue(ex.getMessage().contains("Unknown provider"));
    }

    @Test
    void emptyStringThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> converter.convert(""));
    }

    @Test
    void nullThrowsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> converter.convert(null));
    }

    @Test
    void convertsAllKnownProviders() {
        for (ProviderKind p : ProviderKind.values()) {
            assertNotNull(converter.convert(p.id()), "should convert: " + p.id());
        }
    }
}
