package com.jay.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Registry of supported models and their aliases, used to resolve user-facing
 * model names to concrete provider-specific model entries.
 * <p>
 * Pre-populated with all built-in models across supported providers.
 * Resolution follows a 5-step fallback chain matching the Rust implementation.
 */
public class ModelRegistry {

    private final List<ModelInfo> models;
    private final Map<String, Integer> aliasMap;

    public ModelRegistry(List<ModelInfo> models) {
        this.models = List.copyOf(models);
        this.aliasMap = new HashMap<>();
        for (int i = 0; i < models.size(); i++) {
            var model = models.get(i);
            aliasMap.putIfAbsent(normalize(model.id()), i);
            for (var alias : model.aliases()) {
                aliasMap.putIfAbsent(normalize(alias), i);
            }
        }
    }

    public List<ModelInfo> list() {
        return new ArrayList<>(models);
    }

    /**
     * Resolves a user-requested model name to a concrete ModelInfo.
     * Resolution priority:
     * 1. Ollama provider: passthrough the requested name as-is
     * 2. Provider hint match: find model matching provider + id/alias
     * 3. Passthrough for Atlascloud/Arcee/XiaomiMimo
     * 4. Alias map lookup (case-insensitive)
     * 5. Provider default (first model for hinted provider, or DeepSeek)
     * 6. Global fallback (first model in registry)
     */
    public ModelResolution resolve(String requested, ProviderKind providerHint) {
        var chain = new ArrayList<String>();

        if (requested != null && !requested.isEmpty()) {
            chain.add("requested:" + requested);

            // Step 1: Ollama passthrough
            if (providerHint == ProviderKind.OLLAMA) {
                return new ModelResolution(Optional.ofNullable(requested),
                        new ModelInfo(requested.trim(), ProviderKind.OLLAMA,
                                List.of(), true, false),
                        false, chain);
            }

            // Step 2: Provider hint match
            if (providerHint != null) {
                var match = models.stream()
                        .filter(m -> m.provider() == providerHint && modelMatches(m, requested))
                        .findFirst();
                if (match.isPresent()) {
                    return new ModelResolution(Optional.ofNullable(requested), match.get(), false, chain);
                }
            }

            // Step 3: Passthrough for specific providers
            if (providerHint == ProviderKind.ATLASCLOUD) {
                var passthrough = atlascloudPassthrough(requested);
                if (passthrough != null) {
                    return new ModelResolution(Optional.ofNullable(requested), passthrough, false, chain);
                }
            }
            if (providerHint == ProviderKind.ARCEE) {
                var passthrough = arceePassthrough(requested);
                if (passthrough != null) {
                    return new ModelResolution(Optional.ofNullable(requested), passthrough, false, chain);
                }
            }
            if (providerHint == ProviderKind.XIAOMI_MIMO) {
                var passthrough = xiaomiMimoPassthrough(requested);
                if (passthrough != null) {
                    return new ModelResolution(Optional.ofNullable(requested), passthrough, false, chain);
                }
            }

            // Step 4: Alias map lookup
            var idx = aliasMap.get(normalize(requested));
            if (idx != null) {
                var model = preserveRequestedModelIdCase(models.get(idx), requested);
                return new ModelResolution(Optional.ofNullable(requested), model, false, chain);
            }
        }

        // Step 5: Provider default fallback
        var provider = providerHint != null ? providerHint : ProviderKind.DEEPSEEK;
        chain.add("provider_default:" + provider.id());
        var providerDefault = models.stream()
                .filter(m -> m.provider() == provider)
                .findFirst();
        if (providerDefault.isPresent()) {
            return new ModelResolution(Optional.ofNullable(requested), providerDefault.get(), true, chain);
        }

        // Step 6: Global fallback
        var globalFallback = models.isEmpty()
                ? new ModelInfo("deepseek-v4-pro", ProviderKind.DEEPSEEK, List.of(), true, true)
                : models.getFirst();
        chain.add("global_default:deepseek-v4-pro");
        return new ModelResolution(Optional.ofNullable(requested), globalFallback, true, chain);
    }

    // ---- helpers ----

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean modelMatches(ModelInfo model, String requested) {
        var r = normalize(requested);
        return normalize(model.id()).equals(r)
                || model.aliases().stream().anyMatch(a -> normalize(a).equals(r));
    }

    private static ModelInfo preserveRequestedModelIdCase(ModelInfo model, String requested) {
        var r = requested.trim();
        if (model.id().equalsIgnoreCase(r)) {
            return model.withId(r);
        }
        return model;
    }

    private static ModelInfo atlascloudPassthrough(String requested) {
        var r = requested.trim();
        if (r.isEmpty() || !r.contains("/")) return null;
        return new ModelInfo(r, ProviderKind.ATLASCLOUD, List.of(), true, true);
    }

    private static ModelInfo arceePassthrough(String requested) {
        var r = requested.trim();
        if (r.isEmpty()) return null;
        var supportsReasoning = r.toLowerCase(Locale.ROOT).contains("thinking");
        return new ModelInfo(r, ProviderKind.ARCEE, List.of(), true, supportsReasoning);
    }

    private static ModelInfo xiaomiMimoPassthrough(String requested) {
        var r = requested.trim();
        if (r.isEmpty() || r.chars().anyMatch(c -> Character.isISOControl(c))) return null;
        return new ModelInfo(r, ProviderKind.XIAOMI_MIMO, List.of(), true, true);
    }

    // ---- model family classifier ----

    /**
     * Classify a model identifier by its underlying model family.
     * Equivalent to Rust's model_family() function.
     */
    public static ModelFamily modelFamily(String modelId) {
        var n = normalize(modelId);
        if (n.isEmpty()) return ModelFamily.INFERENCER;
        if (n.contains("deepseek")) return ModelFamily.DEEPSEEK;
        if (n.contains("claude") || n.contains("anthropic")) return ModelFamily.ANTHROPIC;
        if (n.contains("gpt-oss") || n.contains("gpt_oss")) return ModelFamily.GPT_OSS;
        if (n.startsWith("gpt-") || n.contains("/gpt-") || n.contains("openai/"))
            return ModelFamily.OPENAI;
        if (n.contains("gemini") || n.contains("gemma") || n.contains("google/"))
            return ModelFamily.GOOGLE;
        if (n.contains("llama") || n.contains("meta-") || n.contains("meta/"))
            return ModelFamily.META;
        if (n.contains("mistral") || n.contains("mixtral") || n.contains("codestral"))
            return ModelFamily.MISTRAL;
        if (n.contains("qwen")) return ModelFamily.QWEN;
        if (n.contains("grok")) return ModelFamily.GROK;
        if (n.contains("cohere") || n.contains("command-r")) return ModelFamily.COHERE;
        return ModelFamily.INFERENCER;
    }

    // ---- default registry ----

    private static ModelRegistry defaultInstance;

    public static ModelRegistry defaultRegistry() {
        if (defaultInstance == null) {
            defaultInstance = new ModelRegistry(builtinModels());
        }
        return defaultInstance;
    }

    private static ModelInfo m(String id, ProviderKind provider, List<String> aliases,
                                boolean tools, boolean reasoning) {
        return new ModelInfo(id, provider, aliases, tools, reasoning);
    }

    private static List<ModelInfo> builtinModels() {
        return List.of(
                m("deepseek-v4-pro", ProviderKind.DEEPSEEK, List.of(), true, true),
                m("deepseek-v4-flash", ProviderKind.DEEPSEEK,
                        List.of("deepseek-chat", "deepseek-reasoner", "deepseek-r1",
                                "deepseek-v3", "deepseek-v3.2"), true, true),
                m("deepseek-ai/deepseek-v4-pro", ProviderKind.NVIDIA_NIM,
                        List.of("deepseek-v4-pro", "nvidia-deepseek-v4-pro",
                                "nim-deepseek-v4-pro"), true, true),
                m("deepseek-ai/deepseek-v4-flash", ProviderKind.NVIDIA_NIM,
                        List.of("deepseek-v4-flash", "deepseek-chat", "deepseek-reasoner",
                                "nvidia-deepseek-v4-flash", "nim-deepseek-v4-flash"), true, true),
                m("deepseek-v4-pro", ProviderKind.OPENAI,
                        List.of("openai-compatible-deepseek-v4-pro"), true, true),
                m("deepseek-v4-flash", ProviderKind.OPENAI,
                        List.of("openai-compatible-deepseek-v4-flash"), true, true),
                m("deepseek-ai/deepseek-v4-flash", ProviderKind.ATLASCLOUD,
                        List.of("deepseek-v4-flash", "atlascloud-deepseek-v4-flash"), true, true),
                m("deepseek-ai/deepseek-v4-pro", ProviderKind.ATLASCLOUD,
                        List.of("deepseek-v4-pro", "atlascloud-deepseek-v4-pro"), true, true),
                m("deepseek-reasoner", ProviderKind.WANJIE_ARK,
                        List.of("wanjie-deepseek-reasoner", "ark-wanjie-deepseek-reasoner"),
                        true, true),
                m("DeepSeek-V4-Pro", ProviderKind.VOLCENGINE,
                        List.of("deepseek-v4-pro", "volcengine-deepseek-v4-pro",
                                "ark-deepseek-v4-pro"), true, true),
                m("DeepSeek-V4-Flash", ProviderKind.VOLCENGINE,
                        List.of("deepseek-v4-flash", "deepseek-chat",
                                "volcengine-deepseek-v4-flash", "ark-deepseek-v4-flash"),
                        true, true),
                m("trinity-large-thinking", ProviderKind.ARCEE,
                        List.of("trinity", "arcee-trinity", "arcee-trinity-large-thinking"),
                        true, true),
                m("deepseek/deepseek-v4-pro", ProviderKind.OPENROUTER,
                        List.of("deepseek-v4-pro", "openrouter-deepseek-v4-pro"), true, true),
                m("deepseek/deepseek-v4-flash", ProviderKind.OPENROUTER,
                        List.of("deepseek-v4-flash", "deepseek-chat", "deepseek-reasoner",
                                "openrouter-deepseek-v4-flash"), true, true),
                m("arcee-ai/trinity-large-thinking", ProviderKind.OPENROUTER,
                        List.of("trinity", "trinity-large-thinking",
                                "arcee-trinity-large-thinking"), true, true),
                m("xiaomi/mimo-v2.5-pro", ProviderKind.OPENROUTER,
                        List.of("openrouter-mimo-v2.5-pro",
                                "openrouter-xiaomi-mimo-v2.5-pro"), true, true),
                m("xiaomi/mimo-v2.5", ProviderKind.OPENROUTER,
                        List.of("openrouter-mimo-v2.5", "openrouter-xiaomi-mimo-v2.5"),
                        true, true),
                m("qwen/qwen3.6-flash", ProviderKind.OPENROUTER,
                        List.of("qwen3.6-flash", "qwen-3.6-flash"), true, true),
                m("qwen/qwen3.6-35b-a3b", ProviderKind.OPENROUTER,
                        List.of("qwen3.6-35b-a3b", "qwen-3.6-35b-a3b"), true, true),
                m("qwen/qwen3.6-max-preview", ProviderKind.OPENROUTER,
                        List.of("qwen3.6-max-preview", "qwen-3.6-max-preview",
                                "qwen-max-preview"), true, true),
                m("qwen/qwen3.6-27b", ProviderKind.OPENROUTER,
                        List.of("qwen3.6-27b", "qwen-3.6-27b"), true, true),
                m("qwen/qwen3.6-plus", ProviderKind.OPENROUTER,
                        List.of("qwen3.6-plus", "qwen-3.6-plus"), true, true),
                m("moonshotai/kimi-k2.7-code", ProviderKind.OPENROUTER,
                        List.of("kimi-k2.7-code", "openrouter-kimi-k2.7-code"), true, true),
                m("moonshotai/kimi-k2.6", ProviderKind.OPENROUTER,
                        List.of("openrouter-kimi-k2.6"), true, true),
                m("minimax/minimax-m3", ProviderKind.OPENROUTER,
                        List.of("minimax-m3", "minimax-m-3", "openrouter-minimax-m3"), true, true),
                m("z-ai/glm-5.1", ProviderKind.OPENROUTER,
                        List.of("glm-5.1", "zai-glm-5.1"), true, true),
                m("z-ai/glm-5.2", ProviderKind.OPENROUTER,
                        List.of("glm-5.2", "zai-glm-5.2"), true, true),
                m("GLM-5.1", ProviderKind.ZAI,
                        List.of("glm-5.1", "glm-5-1", "zai-glm-5.1", "zai-glm-5-1"), true, true),
                m("GLM-5.2", ProviderKind.ZAI,
                        List.of("glm-5.2", "glm-5-2", "zai-glm-5.2", "zai-glm-5-2"), true, true),
                m("tencent/hy3-preview", ProviderKind.OPENROUTER,
                        List.of("hy3-preview", "tencent-hy3-preview"), true, true),
                m("google/gemma-4-31b-it", ProviderKind.OPENROUTER,
                        List.of("gemma-4-31b", "gemma-4-31b-it"), true, true),
                m("google/gemma-4-26b-a4b-it", ProviderKind.OPENROUTER,
                        List.of("gemma-4-26b-a4b", "gemma-4-26b-a4b-it"), true, true),
                m("nvidia/nemotron-3-nano-omni-30b-a3b-reasoning:free", ProviderKind.OPENROUTER,
                        List.of("nemotron-3-nano-omni", "nemotron-3-nano-omni-reasoning"),
                        true, true),
                m("mimo-v2.5-pro", ProviderKind.XIAOMI_MIMO,
                        List.of("mimo", "pro", "xiaomi-mimo-v2.5-pro",
                                "xiaomi-mimo-v2-5-pro"), true, true),
                m("mimo-v2.5", ProviderKind.XIAOMI_MIMO,
                        List.of("omni", "mimo-omni", "v2.5-omni", "mimo-v2.5-omni",
                                "xiaomi-mimo-v2.5", "xiaomi-mimo-v2.5-omni"), true, true),
                m("mimo-v2.5-asr", ProviderKind.XIAOMI_MIMO,
                        List.of("asr", "speech-to-text", "transcribe"), false, false),
                m("mimo-v2.5-tts", ProviderKind.XIAOMI_MIMO,
                        List.of("tts", "speech", "mimo-tts"), false, false),
                m("mimo-v2.5-tts-voicedesign", ProviderKind.XIAOMI_MIMO,
                        List.of("voicedesign", "voice-design", "mimo-voice-design"), false, false),
                m("mimo-v2.5-tts-voiceclone", ProviderKind.XIAOMI_MIMO,
                        List.of("voiceclone", "voice-clone", "mimo-voice-clone"), false, false),
                m("mimo-v2-tts", ProviderKind.XIAOMI_MIMO,
                        List.of("mimo-v2-speech"), false, false),
                m("deepseek/deepseek-v4-pro", ProviderKind.NOVITA,
                        List.of("deepseek-v4-pro", "novita-deepseek-v4-pro"), true, true),
                m("deepseek/deepseek-v4-flash", ProviderKind.NOVITA,
                        List.of("deepseek-v4-flash", "deepseek-chat", "deepseek-reasoner",
                                "novita-deepseek-v4-flash"), true, true),
                m("accounts/fireworks/models/deepseek-v4-pro", ProviderKind.FIREWORKS,
                        List.of("deepseek-v4-pro", "fireworks-deepseek-v4-pro"), true, true),
                m("deepseek-ai/DeepSeek-V4-Pro", ProviderKind.SILICONFLOW,
                        List.of("deepseek-v4-pro", "deepseek-reasoner", "deepseek-r1",
                                "siliconflow-deepseek-v4-pro"), true, true),
                m("deepseek-ai/DeepSeek-V4-Flash", ProviderKind.SILICONFLOW,
                        List.of("deepseek-v4-flash", "deepseek-chat", "deepseek-v3",
                                "siliconflow-deepseek-v4-flash"), true, true),
                m("trinity-large-preview", ProviderKind.ARCEE,
                        List.of("arcee-trinity-large-preview"), true, false),
                m("kimi-k2.7-code", ProviderKind.MOONSHOT,
                        List.of("kimi", "kimi-k2", "kimi-k2.7", "kimi-code",
                                "moonshot-kimi-k2.7-code"), true, true),
                m("kimi-k2.6", ProviderKind.MOONSHOT,
                        List.of("kimi-k2.6", "moonshot-kimi-k2.6"), true, true),
                m("deepseek-ai/DeepSeek-V4-Pro", ProviderKind.SGLANG,
                        List.of("deepseek-v4-pro", "sglang-deepseek-v4-pro"), true, true),
                m("deepseek-ai/DeepSeek-V4-Flash", ProviderKind.SGLANG,
                        List.of("deepseek-v4-flash", "deepseek-chat", "deepseek-reasoner",
                                "sglang-deepseek-v4-flash"), true, true),
                m("deepseek-ai/DeepSeek-V4-Pro", ProviderKind.VLLM,
                        List.of("deepseek-v4-pro", "vllm-deepseek-v4-pro"), true, true),
                m("deepseek-ai/DeepSeek-V4-Flash", ProviderKind.VLLM,
                        List.of("deepseek-v4-flash", "deepseek-chat", "deepseek-reasoner",
                                "vllm-deepseek-v4-flash"), true, true),
                m("deepseek-coder:1.3b", ProviderKind.OLLAMA, List.of(), true, false),
                m("deepseek-ai/DeepSeek-V4-Pro", ProviderKind.HUGGINGFACE,
                        List.of("deepseek-v4-pro", "hf-deepseek-v4-pro"), true, true),
                m("deepseek-ai/DeepSeek-V4-Flash", ProviderKind.HUGGINGFACE,
                        List.of("deepseek-v4-flash", "deepseek-chat", "deepseek-reasoner",
                                "hf-deepseek-v4-flash"), true, true),
                m("deepseek-ai/DeepSeek-V4-Pro", ProviderKind.TOGETHER,
                        List.of("deepseek-v4-pro", "together-deepseek-v4-pro"), true, true),
                m("deepseek-ai/DeepSeek-V4-Flash", ProviderKind.TOGETHER,
                        List.of("deepseek-v4-flash", "deepseek-chat", "together-deepseek-v4-flash"),
                        true, true),
                m("qwen/qwen3.7-max", ProviderKind.OPENROUTER,
                        List.of("qwen3.7-max", "qwen-3.7-max"), true, true),
                m("gpt-5.5", ProviderKind.OPENAI_CODEX,
                        List.of("codex-gpt-5.5", "chatgpt-gpt-5.5"), true, true),
                m("claude-opus-4-8", ProviderKind.ANTHROPIC,
                        List.of("opus", "claude-opus"), true, true),
                m("claude-sonnet-4-6", ProviderKind.ANTHROPIC,
                        List.of("sonnet", "claude-sonnet"), true, true),
                m("claude-haiku-4-5", ProviderKind.ANTHROPIC,
                        List.of("haiku", "claude-haiku"), true, false),
                m("minimax/minimax-2.7", ProviderKind.OPENROUTER,
                        List.of("minimax-2.7", "minimax-2-7", "openrouter-minimax-2.7"),
                        true, true),
                m("step-3.7-flash", ProviderKind.STEPFUN,
                        List.of("stepfun", "stepflash"), true, false),
                m("MiniMax-M3", ProviderKind.MINIMAX,
                        List.of("minimax", "minimax-m3", "minimax-m-3"), true, true),
                m("MiniMax-M2.7", ProviderKind.MINIMAX,
                        List.of("minimax-m2.7", "minimax-m2-7", "minimax-m-2.7",
                                "minimax-m-2-7"), true, true),
                m("MiniMax-M2.7-highspeed", ProviderKind.MINIMAX,
                        List.of("minimax-m2.7-highspeed", "minimax-m2-7-highspeed",
                                "minimax-m-2.7-highspeed", "minimax-m-2-7-highspeed"),
                        true, true),
                m("MiniMax-M2.5", ProviderKind.MINIMAX,
                        List.of("minimax-m2.5", "minimax-m2-5", "minimax-m-2.5",
                                "minimax-m-2-5"), true, true),
                m("MiniMax-M2.5-highspeed", ProviderKind.MINIMAX,
                        List.of("minimax-m2.5-highspeed", "minimax-m2-5-highspeed",
                                "minimax-m-2.5-highspeed", "minimax-m-2-5-highspeed"),
                        true, true),
                m("MiniMax-M2.1", ProviderKind.MINIMAX,
                        List.of("minimax-m2.1", "minimax-m2-1", "minimax-m-2.1",
                                "minimax-m-2-1"), true, true),
                m("MiniMax-M2.1-highspeed", ProviderKind.MINIMAX,
                        List.of("minimax-m2.1-highspeed", "minimax-m2-1-highspeed",
                                "minimax-m-2.1-highspeed", "minimax-m-2-1-highspeed"),
                        true, true),
                m("MiniMax-M2", ProviderKind.MINIMAX,
                        List.of("minimax-m2", "minimax-m-2"), true, true),
                m("nvidia/nemotron-3-ultra-550b-a55b", ProviderKind.OPENROUTER,
                        List.of("nvidia/nemotron-3-ultra", "nemotron-3-ultra",
                                "nemotron-3-ultra-550b-a55b", "nvidia-nemotron-3-ultra",
                                "nvidia-nemotron-3-ultra-550b-a55b"), true, true),
                m("deepseek-ai/DeepSeek-V4-Pro", ProviderKind.DEEPINFRA,
                        List.of("deepseek-v4-pro", "di-deepseek-v4-pro"), true, true),
                m("deepseek-ai/DeepSeek-V4-Flash", ProviderKind.DEEPINFRA,
                        List.of("deepseek-v4-flash", "di-deepseek-v4-flash"), true, true)
        );
    }
}
