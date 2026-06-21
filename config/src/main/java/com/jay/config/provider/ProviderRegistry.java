package com.jay.config.provider;

import com.jay.agent.ProviderKind;

import java.util.*;

/**
 * Static registry of 25 built-in providers with full metadata.
 * Equivalent to Rust's PROVIDER_REGISTRY array + provider! macro entries.
 */
public final class ProviderRegistry {

    private static final Map<ProviderKind, ProviderMetadata> REGISTRY = new LinkedHashMap<>();

    static {
        register(ProviderKind.DEEPSEEK, "deepseek", "DeepSeek",
            "https://api.deepseek.com/beta", "deepseek-v4-pro",
            List.of("DEEPSEEK_API_KEY"),
            List.of("deepseek-cn", "deepseek-china", "deepseekcn"),
            WireFormat.CHAT_COMPLETIONS);

        register(ProviderKind.NVIDIA_NIM, "nvidia-nim", "NVIDIA NIM",
            "https://integrate.api.nvidia.com/v1", "deepseek-ai/deepseek-v4-pro",
            List.of("NVIDIA_API_KEY", "NVIDIA_NIM_API_KEY", "DEEPSEEK_API_KEY"),
            List.of("nvidia", "nim"),
            WireFormat.CHAT_COMPLETIONS);

        register(ProviderKind.OPENAI, "openai", "OpenAI",
            "https://api.openai.com/v1", "deepseek-v4-pro",
            List.of("OPENAI_API_KEY"),
            List.of("open-ai"),
            WireFormat.CHAT_COMPLETIONS);

        register(ProviderKind.ATLASCLOUD, "atlascloud", "AtlasCloud",
            "https://api.atlascloud.ai/v1", "deepseek-ai/deepseek-v4-flash",
            List.of("ATLASCLOUD_API_KEY"),
            List.of("atlas-cloud", "atlas"),
            WireFormat.CHAT_COMPLETIONS);

        register(ProviderKind.WANJIE_ARK, "wanjie-ark", "Wanjie Ark",
            "https://maas-openapi.wanjiedata.com/api/v1", "deepseek-reasoner",
            List.of("WANJIE_ARK_API_KEY", "WANJIE_API_KEY"),
            List.of("wanjie", "ark-wanjie", "wanjie-maas"),
            WireFormat.CHAT_COMPLETIONS);

        register(ProviderKind.VOLCENGINE, "volcengine", "Volcengine",
            "https://ark.cn-beijing.volces.com/api/coding/v3", "DeepSeek-V4-Pro",
            List.of("VOLCENGINE_API_KEY", "VOLCENGINE_ARK_API_KEY", "ARK_API_KEY"),
            List.of("volcengine-ark", "ark"),
            WireFormat.CHAT_COMPLETIONS);

        register(ProviderKind.OPENROUTER, "openrouter", "OpenRouter",
            "https://openrouter.ai/api/v1", "deepseek/deepseek-v4-pro",
            List.of("OPENROUTER_API_KEY"),
            List.of(),
            WireFormat.CHAT_COMPLETIONS);

        register(ProviderKind.XIAOMI_MIMO, "xiaomi-mimo", "Xiaomi MiMo",
            "https://token-plan-sgp.xiaomimimo.com/v1", "mimo-v2.5-pro",
            List.of("XIAOMI_MIMO_API_KEY", "XIAOMI_API_KEY", "MIMO_API_KEY"),
            List.of("mimo", "xiaomi"),
            WireFormat.CHAT_COMPLETIONS);

        register(ProviderKind.NOVITA, "novita", "Novita",
            "https://api.novita.ai/openai/v1", "deepseek/deepseek-v4-pro",
            List.of("NOVITA_API_KEY"),
            List.of(),
            WireFormat.CHAT_COMPLETIONS);

        register(ProviderKind.FIREWORKS, "fireworks", "Fireworks",
            "https://api.fireworks.ai/inference/v1", "accounts/fireworks/models/deepseek-v4-pro",
            List.of("FIREWORKS_API_KEY"),
            List.of("fireworks-ai"),
            WireFormat.CHAT_COMPLETIONS);

        register(ProviderKind.SILICONFLOW, "siliconflow", "SiliconFlow",
            "https://api.siliconflow.com/v1", "deepseek-ai/DeepSeek-V4-Pro",
            List.of("SILICONFLOW_API_KEY"),
            List.of("silicon-flow"),
            WireFormat.CHAT_COMPLETIONS);

        register(ProviderKind.ARCEE, "arcee", "Arcee",
            "https://api.arcee.ai/api/v1", "trinity-large-thinking",
            List.of("ARCEE_API_KEY"),
            List.of("arcee-ai"),
            WireFormat.CHAT_COMPLETIONS);

        register(ProviderKind.SILICONFLOW_CN, "siliconflow-cn", "SiliconFlow CN",
            "https://api.siliconflow.cn/v1", "deepseek-ai/DeepSeek-V4-Pro",
            List.of("SILICONFLOW_API_KEY"),
            List.of(),
            WireFormat.CHAT_COMPLETIONS);

        register(ProviderKind.MOONSHOT, "moonshot", "Moonshot",
            "https://api.moonshot.ai/v1", "kimi-k2.7-code",
            List.of("MOONSHOT_API_KEY", "KIMI_API_KEY"),
            List.of("kimi", "moonshot-ai"),
            WireFormat.CHAT_COMPLETIONS);

        register(ProviderKind.SGLANG, "sglang", "SGLang",
            "http://localhost:30000/v1", "deepseek-ai/DeepSeek-V4-Pro",
            List.of("SGLANG_API_KEY"),
            List.of("sg-lang"),
            WireFormat.CHAT_COMPLETIONS);

        register(ProviderKind.VLLM, "vllm", "vLLM",
            "http://localhost:8000/v1", "deepseek-ai/DeepSeek-V4-Pro",
            List.of("VLLM_API_KEY"),
            List.of("v-llm"),
            WireFormat.CHAT_COMPLETIONS);

        register(ProviderKind.OLLAMA, "ollama", "Ollama",
            "http://localhost:11434/v1", "deepseek-coder:1.3b",
            List.of("OLLAMA_API_KEY"),
            List.of("ollama-local"),
            WireFormat.CHAT_COMPLETIONS);

        register(ProviderKind.HUGGINGFACE, "huggingface", "HuggingFace",
            "https://router.huggingface.co/v1", "deepseek-ai/DeepSeek-V4-Pro",
            List.of("HF_API_KEY", "HUGGINGFACE_API_KEY"),
            List.of("hf", "hugging-face"),
            WireFormat.CHAT_COMPLETIONS);

        register(ProviderKind.TOGETHER, "together", "Together",
            "https://api.together.xyz/v1", "deepseek-ai/DeepSeek-V4-Pro",
            List.of("TOGETHER_API_KEY"),
            List.of("together-ai"),
            WireFormat.CHAT_COMPLETIONS);

        register(ProviderKind.OPENAI_CODEX, "openai-codex", "OpenAI Codex",
            "https://chatgpt.com/backend-api", "gpt-5.5",
            List.of("OPENAI_CODEX_API_KEY", "OPENAI_API_KEY"),
            List.of("codex", "chatgpt"),
            WireFormat.RESPONSES);

        register(ProviderKind.ANTHROPIC, "anthropic", "Anthropic",
            "https://api.anthropic.com", "claude-sonnet-4-6",
            List.of("ANTHROPIC_API_KEY"),
            List.of("claude"),
            WireFormat.ANTHROPIC_MESSAGES);

        register(ProviderKind.ZAI, "z-ai", "Z.ai",
            "https://api.z.ai/api/coding/paas/v4", "GLM-5.1",
            List.of("ZAI_API_KEY"),
            List.of("z-ai", "z.ai"),
            WireFormat.CHAT_COMPLETIONS);

        register(ProviderKind.STEPFUN, "stepfun", "StepFun",
            "https://api.stepfun.ai/v1", "step-3.7-flash",
            List.of("STEPFUN_API_KEY"),
            List.of("step-fun", "stepflash"),
            WireFormat.CHAT_COMPLETIONS);

        register(ProviderKind.MINIMAX, "minimax", "MiniMax",
            "https://api.minimax.io/v1", "MiniMax-M3",
            List.of("MINIMAX_API_KEY"),
            List.of("mini-max"),
            WireFormat.CHAT_COMPLETIONS);

        register(ProviderKind.DEEPINFRA, "deepinfra", "DeepInfra",
            "https://api.deepinfra.com/v1/openai", "deepseek-ai/DeepSeek-V4-Pro",
            List.of("DEEPINFRA_API_KEY"),
            List.of("deep-infra"),
            WireFormat.CHAT_COMPLETIONS);
    }

    private static void register(ProviderKind kind, String id, String displayName,
                                  String defaultBaseUrl, String defaultModel,
                                  List<String> envVars, List<String> aliases,
                                  WireFormat wireFormat) {
        REGISTRY.put(kind, new ProviderMetadata(kind, id, displayName,
            defaultBaseUrl, defaultModel, envVars, aliases, wireFormat));
    }

    public static ProviderMetadata get(ProviderKind kind) {
        return REGISTRY.get(kind);
    }

    public static List<ProviderMetadata> all() {
        return List.copyOf(REGISTRY.values());
    }

    public static int size() {
        return REGISTRY.size();
    }
}
