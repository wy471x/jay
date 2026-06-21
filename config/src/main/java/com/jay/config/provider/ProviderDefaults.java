package com.jay.config.provider;

import com.jay.agent.ProviderKind;

import java.util.Map;

/**
 * Default models and base URLs for all 25 built-in providers.
 * Equivalent to Rust's provider default constants.
 */
public final class ProviderDefaults {

    private static final Map<ProviderKind, ProviderEntry> DEFAULTS = Map.<ProviderKind, ProviderEntry>ofEntries(
        entry(ProviderKind.DEEPSEEK, "deepseek-v4-pro", "https://api.deepseek.com/beta"),
        entry(ProviderKind.NVIDIA_NIM, "deepseek-ai/deepseek-v4-pro", "https://integrate.api.nvidia.com/v1"),
        entry(ProviderKind.OPENAI, "deepseek-v4-pro", "https://api.openai.com/v1"),
        entry(ProviderKind.ATLASCLOUD, "deepseek-ai/deepseek-v4-flash", "https://api.atlascloud.ai/v1"),
        entry(ProviderKind.WANJIE_ARK, "deepseek-reasoner", "https://maas-openapi.wanjiedata.com/api/v1"),
        entry(ProviderKind.VOLCENGINE, "DeepSeek-V4-Pro", "https://ark.cn-beijing.volces.com/api/coding/v3"),
        entry(ProviderKind.OPENROUTER, "deepseek/deepseek-v4-pro", "https://openrouter.ai/api/v1"),
        entry(ProviderKind.XIAOMI_MIMO, "mimo-v2.5-pro", "https://token-plan-sgp.xiaomimimo.com/v1"),
        entry(ProviderKind.NOVITA, "deepseek/deepseek-v4-pro", "https://api.novita.ai/openai/v1"),
        entry(ProviderKind.FIREWORKS, "accounts/fireworks/models/deepseek-v4-pro", "https://api.fireworks.ai/inference/v1"),
        entry(ProviderKind.SILICONFLOW, "deepseek-ai/DeepSeek-V4-Pro", "https://api.siliconflow.com/v1"),
        entry(ProviderKind.ARCEE, "trinity-large-thinking", "https://api.arcee.ai/api/v1"),
        entry(ProviderKind.SILICONFLOW_CN, "deepseek-ai/DeepSeek-V4-Pro", "https://api.siliconflow.cn/v1"),
        entry(ProviderKind.MOONSHOT, "kimi-k2.7-code", "https://api.moonshot.ai/v1"),
        entry(ProviderKind.SGLANG, "deepseek-ai/DeepSeek-V4-Pro", "http://localhost:30000/v1"),
        entry(ProviderKind.VLLM, "deepseek-ai/DeepSeek-V4-Pro", "http://localhost:8000/v1"),
        entry(ProviderKind.OLLAMA, "deepseek-coder:1.3b", "http://localhost:11434/v1"),
        entry(ProviderKind.HUGGINGFACE, "deepseek-ai/DeepSeek-V4-Pro", "https://router.huggingface.co/v1"),
        entry(ProviderKind.TOGETHER, "deepseek-ai/DeepSeek-V4-Pro", "https://api.together.xyz/v1"),
        entry(ProviderKind.OPENAI_CODEX, "gpt-5.5", "https://chatgpt.com/backend-api"),
        entry(ProviderKind.ANTHROPIC, "claude-sonnet-4-6", "https://api.anthropic.com"),
        entry(ProviderKind.ZAI, "GLM-5.1", "https://api.z.ai/api/coding/paas/v4"),
        entry(ProviderKind.STEPFUN, "step-3.7-flash", "https://api.stepfun.ai/v1"),
        entry(ProviderKind.MINIMAX, "MiniMax-M3", "https://api.minimax.io/v1"),
        entry(ProviderKind.DEEPINFRA, "deepseek-ai/DeepSeek-V4-Pro", "https://api.deepinfra.com/v1/openai")
    );

    private ProviderDefaults() {}

    public static String defaultModel(ProviderKind kind) {
        ProviderEntry e = DEFAULTS.get(kind);
        return e != null ? e.model : "deepseek-v4-pro";
    }

    public static String defaultBaseUrl(ProviderKind kind) {
        ProviderEntry e = DEFAULTS.get(kind);
        return e != null ? e.baseUrl : "https://api.deepseek.com/beta";
    }

    /** Local/self-hosted providers that should skip the secret store. */
    public static boolean shouldSkipSecretStore(ProviderKind kind) {
        return kind == ProviderKind.SGLANG
            || kind == ProviderKind.VLLM
            || kind == ProviderKind.OLLAMA;
    }

    private static Map.Entry<ProviderKind, ProviderEntry> entry(
            ProviderKind kind, String model, String baseUrl) {
        return Map.entry(kind, new ProviderEntry(model, baseUrl));
    }

    private record ProviderEntry(String model, String baseUrl) {}
}
