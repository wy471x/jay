package com.jay.secrets;

import java.util.List;
import java.util.Map;

/**
 * Maps canonical provider names to their environment variable(s).
 * Equivalent to Rust's {@code env_for()} function.
 */
public final class ProviderEnv {

    private static final Map<String, List<String>> PROVIDER_ENV_VARS = Map.ofEntries(
        Map.entry("deepseek", List.of("DEEPSEEK_API_KEY")),
        Map.entry("openrouter", List.of("OPENROUTER_API_KEY")),
        Map.entry("xiaomi-mimo", List.of("XIAOMI_MIMO_API_KEY", "XIAOMI_API_KEY", "MIMO_API_KEY")),
        Map.entry("novita", List.of("NOVITA_API_KEY")),
        Map.entry("nvidia", List.of("NVIDIA_API_KEY", "NVIDIA_NIM_API_KEY", "DEEPSEEK_API_KEY")),
        Map.entry("nvidia-nim", List.of("NVIDIA_API_KEY", "NVIDIA_NIM_API_KEY", "DEEPSEEK_API_KEY")),
        Map.entry("fireworks", List.of("FIREWORKS_API_KEY")),
        Map.entry("siliconflow", List.of("SILICONFLOW_API_KEY")),
        Map.entry("arcee", List.of("ARCEE_API_KEY")),
        Map.entry("moonshot", List.of("MOONSHOT_API_KEY", "KIMI_API_KEY")),
        Map.entry("sglang", List.of("SGLANG_API_KEY")),
        Map.entry("vllm", List.of("VLLM_API_KEY")),
        Map.entry("ollama", List.of("OLLAMA_API_KEY")),
        Map.entry("openai", List.of("OPENAI_API_KEY")),
        Map.entry("anthropic", List.of("ANTHROPIC_API_KEY")),
        Map.entry("claude", List.of("ANTHROPIC_API_KEY")),
        Map.entry("atlascloud", List.of("ATLASCLOUD_API_KEY")),
        Map.entry("volcengine", List.of("VOLCENGINE_API_KEY", "VOLCENGINE_ARK_API_KEY", "ARK_API_KEY")),
        Map.entry("wanjie", List.of("WANJIE_ARK_API_KEY", "WANJIE_API_KEY", "WANJIE_MAAS_API_KEY")),
        Map.entry("minimax", List.of("MINIMAX_API_KEY")),
        Map.entry("stepfun", List.of("STEPFUN_API_KEY")),
        Map.entry("zai", List.of("ZAI_API_KEY")),
        Map.entry("deepinfra", List.of("DEEPINFRA_API_KEY")),
        Map.entry("together", List.of("TOGETHER_API_KEY")),
        Map.entry("huggingface", List.of("HF_API_KEY", "HUGGINGFACE_API_KEY")),
        Map.entry("openai-codex", List.of("OPENAI_CODEX_API_KEY", "OPENAI_API_KEY"))
    );

    // Provider alias normalization
    private static final Map<String, String> ALIASES = Map.ofEntries(
        Map.entry("xiaomi_mimo", "xiaomi-mimo"), Map.entry("xiaomimimo", "xiaomi-mimo"),
        Map.entry("mimo", "xiaomi-mimo"), Map.entry("xiaomi", "xiaomi-mimo"),
        Map.entry("nvidia-nim", "nvidia"), Map.entry("nvidia_nim", "nvidia"),
        Map.entry("nim", "nvidia"),
        Map.entry("fireworks-ai", "fireworks"),
        Map.entry("silicon-flow", "siliconflow"), Map.entry("silicon_flow", "siliconflow"),
        Map.entry("siliconflow-cn", "siliconflow"),
        Map.entry("arcee-ai", "arcee"), Map.entry("arcee_ai", "arcee"),
        Map.entry("moonshot-ai", "moonshot"), Map.entry("kimi", "moonshot"),
        Map.entry("kimi-k2", "moonshot"),
        Map.entry("sg-lang", "sglang"),
        Map.entry("v-llm", "vllm"),
        Map.entry("ollama-local", "ollama"),
        Map.entry("anthropic", "claude"),
        Map.entry("atlas-cloud", "atlascloud"), Map.entry("atlas_cloud", "atlascloud"),
        Map.entry("atlas", "atlascloud"),
        Map.entry("volcengine-ark", "volcengine"), Map.entry("volcengine_ark", "volcengine"),
        Map.entry("ark", "volcengine"), Map.entry("volc-ark", "volcengine"),
        Map.entry("volcengineark", "volcengine"),
        Map.entry("wanjie-ark", "wanjie"), Map.entry("wanjie_ark", "wanjie"),
        Map.entry("ark-wanjie", "wanjie"), Map.entry("ark_wanjie", "wanjie"),
        Map.entry("wanjieark", "wanjie"), Map.entry("wanjie-maas", "wanjie"),
        Map.entry("wanjie_maas", "wanjie"), Map.entry("wanjiemaas", "wanjie"),
        Map.entry("deep-infra", "deepinfra"),
        Map.entry("hugging-face", "huggingface"), Map.entry("hf", "huggingface"),
        Map.entry("codex", "openai-codex")
    );

    private ProviderEnv() { }

    /**
     * Resolve the first non-empty environment variable for a provider name.
     * Handles both canonical names and aliases (case-insensitive).
     */
    public static String envFor(String name) {
        if (name == null || name.isBlank()) return null;
        String key = name.toLowerCase().replace('-', '_').replace(' ', '_').trim();

        // Check direct match
        List<String> vars = PROVIDER_ENV_VARS.get(key);
        if (vars == null) {
            // Try alias resolution
            String canonical = ALIASES.get(key);
            if (canonical != null) {
                vars = PROVIDER_ENV_VARS.get(canonical);
            }
        }
        if (vars == null) return null;

        for (String var : vars) {
            String val = System.getenv(var);
            if (val != null && !val.isBlank()) return val;
        }
        return null;
    }

    /** All supported provider names (canonical + aliases). Exposed for testing. */
    static int knownProviderCount() {
        return PROVIDER_ENV_VARS.size();
    }
}
