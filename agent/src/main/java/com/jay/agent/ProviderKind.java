package com.jay.agent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum of all supported AI model providers — equivalent to Rust's ProviderKind.
 * Each variant maps to the provider's canonical string identifier.
 * Jackson-annotated for serde-equivalent serialization.
 */
public enum ProviderKind {
    DEEPSEEK("deepseek"),
    NVIDIA_NIM("nvidia-nim"),
    OPENAI("openai"),
    ATLASCLOUD("atlascloud"),
    WANJIE_ARK("wanjie-ark"),
    VOLCENGINE("volcengine"),
    OPENROUTER("openrouter"),
    XIAOMI_MIMO("xiaomi-mimo"),
    NOVITA("novita"),
    FIREWORKS("fireworks"),
    SILICONFLOW("siliconflow"),
    SILICONFLOW_CN("siliconflow-cn"),
    ARCEE("arcee"),
    MOONSHOT("moonshot"),
    SGLANG("sglang"),
    VLLM("vllm"),
    OLLAMA("ollama"),
    HUGGINGFACE("huggingface"),
    TOGETHER("together"),
    OPENAI_CODEX("openai-codex"),
    ANTHROPIC("anthropic"),
    ZAI("z-ai"),
    STEPFUN("stepfun"),
    MINIMAX("minimax"),
    DEEPINFRA("deepinfra");

    private final String id;

    ProviderKind(String id) { this.id = id; }

    @JsonValue
    public String id() { return id; }

    @JsonCreator
    public static ProviderKind fromString(String value) {
        return parse(value);
    }

    public static ProviderKind parse(String value) {
        var trimmed = value.trim().toLowerCase();
        for (var p : values()) {
            if (p.id.equals(trimmed)) return p;
        }
        return null;
    }
}
