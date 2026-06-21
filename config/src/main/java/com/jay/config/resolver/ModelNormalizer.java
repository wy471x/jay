package com.jay.config.resolver;

import com.jay.agent.ProviderKind;

import java.util.*;

/**
 * Short model-name alias → provider-specific fully-qualified model ID.
 * Equivalent to Rust's normalize_model_for_provider().
 */
public final class ModelNormalizer {

    private static final Map<ProviderKind, Map<String, String>> ALIAS_MAP;

    static {
        Map<ProviderKind, Map<String, String>> map = new HashMap<>();

        map.put(ProviderKind.DEEPSEEK, Map.of(
            "v4-pro", "deepseek-v4-pro", "v4-flash", "deepseek-v4-flash",
            "chat", "deepseek-chat", "reasoner", "deepseek-reasoner"));

        map.put(ProviderKind.NVIDIA_NIM, Map.of(
            "deepseek-v4-pro", "deepseek-ai/deepseek-v4-pro",
            "deepseek-v4-flash", "deepseek-ai/deepseek-v4-flash"));

        // OpenRouter: 14 entries, exceeds Map.of() limit
        Map<String, String> openrouter = new HashMap<>();
        openrouter.put("deepseek-v4-pro", "deepseek/deepseek-v4-pro");
        openrouter.put("deepseek-v4-flash", "deepseek/deepseek-v4-flash");
        openrouter.put("trinity", "arcee-ai/trinity-large-thinking");
        openrouter.put("gemma-4-31b", "google/gemma-4-31b-it");
        openrouter.put("glm-5.1", "z-ai/glm-5.1");
        openrouter.put("glm-5.2", "z-ai/glm-5.2");
        openrouter.put("kimi-k2.7-code", "moonshotai/kimi-k2.7-code");
        openrouter.put("kimi-k2.6", "moonshotai/kimi-k2.6");
        openrouter.put("minimax-m3", "minimax/minimax-m3");
        openrouter.put("minimax-2.7", "minimax/minimax-2.7");
        openrouter.put("qwen3.6-flash", "qwen/qwen3.6-flash");
        openrouter.put("qwen3.7-max", "qwen/qwen3.7-max");
        openrouter.put("mimo-v2.5-pro", "xiaomi/mimo-v2.5-pro");
        openrouter.put("mimo-v2.5", "xiaomi/mimo-v2.5");
        map.put(ProviderKind.OPENROUTER, Collections.unmodifiableMap(openrouter));

        map.put(ProviderKind.FIREWORKS, Map.of(
            "deepseek-v4-pro", "accounts/fireworks/models/deepseek-v4-pro"));

        map.put(ProviderKind.SILICONFLOW, Map.of(
            "deepseek-v4-pro", "deepseek-ai/DeepSeek-V4-Pro",
            "deepseek-v4-flash", "deepseek-ai/DeepSeek-V4-Flash"));

        map.put(ProviderKind.HUGGINGFACE, Map.of(
            "deepseek-v4-pro", "deepseek-ai/DeepSeek-V4-Pro",
            "deepseek-v4-flash", "deepseek-ai/DeepSeek-V4-Flash"));

        map.put(ProviderKind.TOGETHER, Map.of(
            "deepseek-v4-pro", "deepseek-ai/DeepSeek-V4-Pro",
            "deepseek-v4-flash", "deepseek-ai/DeepSeek-V4-Flash"));

        map.put(ProviderKind.DEEPINFRA, Map.of(
            "deepseek-v4-pro", "deepseek-ai/DeepSeek-V4-Pro",
            "deepseek-v4-flash", "deepseek-ai/DeepSeek-V4-Flash"));

        map.put(ProviderKind.MINIMAX, Map.of(
            "m3", "MiniMax-M3", "m2.7", "MiniMax-M2.7",
            "m2.7-highspeed", "MiniMax-M2.7-highspeed",
            "m2.5", "MiniMax-M2.5", "m2.1", "MiniMax-M2.1", "m2", "MiniMax-M2"));

        map.put(ProviderKind.ZAI, Map.of("glm-5.1", "GLM-5.1", "glm-5.2", "GLM-5.2"));

        map.put(ProviderKind.XIAOMI_MIMO, Map.of(
            "pro", "mimo-v2.5-pro", "omni", "mimo-v2.5",
            "asr", "mimo-v2.5-asr", "tts", "mimo-v2.5-tts",
            "voice-design", "mimo-v2.5-tts-voicedesign",
            "voice-clone", "mimo-v2.5-tts-voiceclone", "v2-tts", "mimo-v2-tts"));

        ALIAS_MAP = Collections.unmodifiableMap(map);
    }

    private ModelNormalizer() {}

    public static String normalize(String alias, ProviderKind provider) {
        if (alias == null || alias.isBlank()) return alias;
        String key = alias.toLowerCase().trim();
        Map<String, String> providerAliases = ALIAS_MAP.get(provider);
        if (providerAliases != null) {
            String resolved = providerAliases.get(key);
            if (resolved != null) return resolved;
        }
        return alias;
    }
}
