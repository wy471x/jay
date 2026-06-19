package com.jay.agent;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ModelRegistryTest {

    private static ModelRegistry registry;

    @BeforeAll
    static void setUp() {
        registry = ModelRegistry.defaultRegistry();
    }

    // ---- basic resolution ----

    @Test
    void deepseekV4ProAliasStaysDeepseekByDefault() {
        var r = registry.resolve("deepseek-v4-pro", null);
        assertEquals(ProviderKind.DEEPSEEK, r.resolved().provider());
        assertEquals("deepseek-v4-pro", r.resolved().id());
    }

    @Test
    void deepseekV4ProAliasResolvesToNvidiaNimWhenProviderHinted() {
        var r = registry.resolve("deepseek-v4-pro", ProviderKind.NVIDIA_NIM);
        assertEquals(ProviderKind.NVIDIA_NIM, r.resolved().provider());
        assertEquals("deepseek-ai/deepseek-v4-pro", r.resolved().id());
    }

    @Test
    void nvidiaNimDefaultUsesCatalogModelId() {
        var r = registry.resolve(null, ProviderKind.NVIDIA_NIM);
        assertEquals(ProviderKind.NVIDIA_NIM, r.resolved().provider());
        assertEquals("deepseek-ai/deepseek-v4-pro", r.resolved().id());
    }

    @Test
    void deepseekV4FlashAliasResolvesToNvidiaNimWhenProviderHinted() {
        var r = registry.resolve("deepseek-v4-flash", ProviderKind.NVIDIA_NIM);
        assertEquals(ProviderKind.NVIDIA_NIM, r.resolved().provider());
        assertEquals("deepseek-ai/deepseek-v4-flash", r.resolved().id());
    }

    // ---- atlascloud ----

    @Test
    void atlascloudDefaultUsesNamespacedModelId() {
        var r = registry.resolve(null, ProviderKind.ATLASCLOUD);
        assertEquals(ProviderKind.ATLASCLOUD, r.resolved().provider());
        assertEquals("deepseek-ai/deepseek-v4-flash", r.resolved().id());
        assertTrue(r.resolved().supportsReasoning());
    }

    @Test
    void deepseekV4FlashAliasResolvesToAtlascloudWhenProviderHinted() {
        var r = registry.resolve("deepseek-v4-flash", ProviderKind.ATLASCLOUD);
        assertEquals(ProviderKind.ATLASCLOUD, r.resolved().provider());
        assertEquals("deepseek-ai/deepseek-v4-flash", r.resolved().id());
    }

    @Test
    void deepseekV4ProAliasResolvesToAtlascloudWhenProviderHinted() {
        var r = registry.resolve("deepseek-v4-pro", ProviderKind.ATLASCLOUD);
        assertEquals(ProviderKind.ATLASCLOUD, r.resolved().provider());
        assertEquals("deepseek-ai/deepseek-v4-pro", r.resolved().id());
    }

    @Test
    void atlascloudProviderHintPassesThroughExplicitModelId() {
        var r = registry.resolve("openai/gpt-5.2-chat", ProviderKind.ATLASCLOUD);
        assertEquals(ProviderKind.ATLASCLOUD, r.resolved().provider());
        assertEquals("openai/gpt-5.2-chat", r.resolved().id());
        assertTrue(r.resolved().supportsTools());
        assertTrue(r.resolved().supportsReasoning());
        assertFalse(r.usedFallback());
    }

    @Test
    void atlascloudProviderHintPreservesExplicitModelIdCase() {
        var r = registry.resolve("Qwen/Qwen3-Coder", ProviderKind.ATLASCLOUD);
        assertEquals(ProviderKind.ATLASCLOUD, r.resolved().provider());
        assertEquals("Qwen/Qwen3-Coder", r.resolved().id());
        assertFalse(r.usedFallback());
    }

    @Test
    void atlascloudPlainUnknownModelStillUsesProviderDefault() {
        var r = registry.resolve("not-in-atlas", ProviderKind.ATLASCLOUD);
        assertEquals(ProviderKind.ATLASCLOUD, r.resolved().provider());
        assertEquals("deepseek-ai/deepseek-v4-flash", r.resolved().id());
        assertTrue(r.usedFallback());
    }

    // ---- openrouter ----

    @Test
    void openrouterDefaultUsesNamespacedModelId() {
        var r = registry.resolve(null, ProviderKind.OPENROUTER);
        assertEquals(ProviderKind.OPENROUTER, r.resolved().provider());
        assertEquals("deepseek/deepseek-v4-pro", r.resolved().id());
    }

    @Test
    void deepseekV4FlashAliasResolvesToOpenrouterWhenProviderHinted() {
        var r = registry.resolve("deepseek-v4-flash", ProviderKind.OPENROUTER);
        assertEquals(ProviderKind.OPENROUTER, r.resolved().provider());
        assertEquals("deepseek/deepseek-v4-flash", r.resolved().id());
    }

    @Test
    void recentOpenrouterLargeModelAliasesResolveWhenProviderHinted() {
        var cases = new Object[][]{
                {"trinity-large-thinking", "arcee-ai/trinity-large-thinking"},
                {"qwen3.6-flash", "qwen/qwen3.6-flash"},
                {"qwen3.6-35b-a3b", "qwen/qwen3.6-35b-a3b"},
                {"qwen3.6-max-preview", "qwen/qwen3.6-max-preview"},
                {"qwen3.6-plus", "qwen/qwen3.6-plus"},
                {"gemma-4-31b-it", "google/gemma-4-31b-it"},
                {"glm-5.1", "z-ai/glm-5.1"},
                {"glm-5.2", "z-ai/glm-5.2"},
                {"minimax-m3", "minimax/minimax-m3"},
                {"minimax-2.7", "minimax/minimax-2.7"},
                {"openrouter-mimo-v2.5-pro", "xiaomi/mimo-v2.5-pro"},
                {"openrouter-kimi-k2.7-code", "moonshotai/kimi-k2.7-code"},
                {"openrouter-kimi-k2.6", "moonshotai/kimi-k2.6"},
                {"nemotron-3-ultra", "nvidia/nemotron-3-ultra-550b-a55b"},
                {"nvidia/nemotron-3-ultra", "nvidia/nemotron-3-ultra-550b-a55b"},
        };
        for (var c : cases) {
            var alias = (String) c[0];
            var expected = (String) c[1];
            var r = registry.resolve(alias, ProviderKind.OPENROUTER);
            assertEquals(ProviderKind.OPENROUTER, r.resolved().provider(), "alias: " + alias);
            assertEquals(expected, r.resolved().id(), "alias: " + alias);
            assertTrue(r.resolved().supportsTools(), "alias: " + alias);
            assertTrue(r.resolved().supportsReasoning(), "alias: " + alias);
        }
    }

    // ---- xiaomi mimo ----

    @Test
    void xiaomiMimoDefaultUsesCanonicalModelId() {
        var r = registry.resolve(null, ProviderKind.XIAOMI_MIMO);
        assertEquals(ProviderKind.XIAOMI_MIMO, r.resolved().provider());
        assertEquals("mimo-v2.5-pro", r.resolved().id());
        assertTrue(r.resolved().supportsReasoning());
    }

    @Test
    void xiaomiMimoTtsAliasesResolveWhenProviderHinted() {
        var r = registry.resolve("tts", ProviderKind.XIAOMI_MIMO);
        assertEquals(ProviderKind.XIAOMI_MIMO, r.resolved().provider());
        assertEquals("mimo-v2.5-tts", r.resolved().id());
        assertFalse(r.resolved().supportsTools());
        assertFalse(r.resolved().supportsReasoning());

        r = registry.resolve("voice-design", ProviderKind.XIAOMI_MIMO);
        assertEquals("mimo-v2.5-tts-voicedesign", r.resolved().id());

        r = registry.resolve("voiceclone", ProviderKind.XIAOMI_MIMO);
        assertEquals("mimo-v2.5-tts-voiceclone", r.resolved().id());
    }

    @Test
    void xiaomiMimoChatAliasesResolveWhenProviderHinted() {
        var r = registry.resolve("omni", ProviderKind.XIAOMI_MIMO);
        assertEquals(ProviderKind.XIAOMI_MIMO, r.resolved().provider());
        assertEquals("mimo-v2.5", r.resolved().id());
        assertTrue(r.resolved().supportsTools());
    }

    @Test
    void xiaomiMimoProviderHintPreservesCustomModelId() {
        var r = registry.resolve("account-custom-mimo", ProviderKind.XIAOMI_MIMO);
        assertEquals(ProviderKind.XIAOMI_MIMO, r.resolved().provider());
        assertEquals("account-custom-mimo", r.resolved().id());
        assertFalse(r.usedFallback());
    }

    @Test
    void xiaomiMimoProviderHintDoesNotReclassifyOpenrouterModelId() {
        var r = registry.resolve("deepseek/deepseek-v4-pro", ProviderKind.XIAOMI_MIMO);
        assertEquals(ProviderKind.XIAOMI_MIMO, r.resolved().provider());
        assertEquals("deepseek/deepseek-v4-pro", r.resolved().id());
        assertFalse(r.usedFallback());
    }

    // ---- moonshot ----

    @Test
    void moonshotDefaultAndAliasesUseKimiK27Code() {
        for (var requested : new String[]{null, "kimi", "kimi-k2.7-code"}) {
            var r = registry.resolve(requested, ProviderKind.MOONSHOT);
            assertEquals(ProviderKind.MOONSHOT, r.resolved().provider());
            assertEquals("kimi-k2.7-code", r.resolved().id());
            assertTrue(r.resolved().supportsTools());
            assertTrue(r.resolved().supportsReasoning());
        }
    }

    @Test
    void moonshotExplicitKimiK26RemainsAvailable() {
        var r = registry.resolve("kimi-k2.6", ProviderKind.MOONSHOT);
        assertEquals(ProviderKind.MOONSHOT, r.resolved().provider());
        assertEquals("kimi-k2.6", r.resolved().id());
        assertTrue(r.resolved().supportsReasoning());
    }

    // ---- wanjie ark ----

    @Test
    void wanjieArkDefaultUsesReasonerModelId() {
        var r = registry.resolve(null, ProviderKind.WANJIE_ARK);
        assertEquals(ProviderKind.WANJIE_ARK, r.resolved().provider());
        assertEquals("deepseek-reasoner", r.resolved().id());
        assertTrue(r.resolved().supportsReasoning());
    }

    // ---- novita ----

    @Test
    void novitaDefaultUsesNamespacedModelId() {
        var r = registry.resolve(null, ProviderKind.NOVITA);
        assertEquals(ProviderKind.NOVITA, r.resolved().provider());
        assertEquals("deepseek/deepseek-v4-pro", r.resolved().id());
    }

    @Test
    void deepseekV4FlashAliasResolvesToNovitaWhenProviderHinted() {
        var r = registry.resolve("deepseek-v4-flash", ProviderKind.NOVITA);
        assertEquals(ProviderKind.NOVITA, r.resolved().provider());
        assertEquals("deepseek/deepseek-v4-flash", r.resolved().id());
    }

    // ---- fireworks ----

    @Test
    void fireworksDefaultUsesCanonicalModelId() {
        var r = registry.resolve(null, ProviderKind.FIREWORKS);
        assertEquals(ProviderKind.FIREWORKS, r.resolved().provider());
        assertEquals("accounts/fireworks/models/deepseek-v4-pro", r.resolved().id());
    }

    // ---- siliconflow ----

    @Test
    void siliconflowDefaultUsesCanonicalProModelId() {
        var r = registry.resolve(null, ProviderKind.SILICONFLOW);
        assertEquals(ProviderKind.SILICONFLOW, r.resolved().provider());
        assertEquals("deepseek-ai/DeepSeek-V4-Pro", r.resolved().id());
        assertTrue(r.resolved().supportsReasoning());
    }

    @Test
    void deepseekReasonerAliasResolvesToSiliconflowProWhenProviderHinted() {
        var r = registry.resolve("deepseek-reasoner", ProviderKind.SILICONFLOW);
        assertEquals(ProviderKind.SILICONFLOW, r.resolved().provider());
        assertEquals("deepseek-ai/DeepSeek-V4-Pro", r.resolved().id());
    }

    @Test
    void deepseekV4FlashAliasResolvesToSiliconflowFlashWhenProviderHinted() {
        var r = registry.resolve("deepseek-v4-flash", ProviderKind.SILICONFLOW);
        assertEquals(ProviderKind.SILICONFLOW, r.resolved().provider());
        assertEquals("deepseek-ai/DeepSeek-V4-Flash", r.resolved().id());
    }

    // ---- sglang ----

    @Test
    void sglangDefaultUsesCanonicalModelId() {
        var r = registry.resolve(null, ProviderKind.SGLANG);
        assertEquals(ProviderKind.SGLANG, r.resolved().provider());
        assertEquals("deepseek-ai/DeepSeek-V4-Pro", r.resolved().id());
    }

    @Test
    void deepseekV4FlashAliasResolvesToSglangWhenProviderHinted() {
        var r = registry.resolve("deepseek-v4-flash", ProviderKind.SGLANG);
        assertEquals(ProviderKind.SGLANG, r.resolved().provider());
        assertEquals("deepseek-ai/DeepSeek-V4-Flash", r.resolved().id());
    }

    // ---- vllm ----

    @Test
    void vllmDefaultUsesCanonicalModelId() {
        var r = registry.resolve(null, ProviderKind.VLLM);
        assertEquals(ProviderKind.VLLM, r.resolved().provider());
        assertEquals("deepseek-ai/DeepSeek-V4-Pro", r.resolved().id());
    }

    @Test
    void deepseekV4FlashAliasResolvesToVllmWhenProviderHinted() {
        var r = registry.resolve("deepseek-v4-flash", ProviderKind.VLLM);
        assertEquals(ProviderKind.VLLM, r.resolved().provider());
        assertEquals("deepseek-ai/DeepSeek-V4-Flash", r.resolved().id());
    }

    // ---- ollama ----

    @Test
    void ollamaDefaultUsesSmallLocalModelId() {
        var r = registry.resolve(null, ProviderKind.OLLAMA);
        assertEquals(ProviderKind.OLLAMA, r.resolved().provider());
        assertEquals("deepseek-coder:1.3b", r.resolved().id());
        assertFalse(r.resolved().supportsReasoning());
    }

    @Test
    void ollamaRequestedModelTagIsPreserved() {
        var r = registry.resolve("qwen2.5-coder:7b", ProviderKind.OLLAMA);
        assertEquals(ProviderKind.OLLAMA, r.resolved().provider());
        assertEquals("qwen2.5-coder:7b", r.resolved().id());
        assertFalse(r.usedFallback());
    }

    // ---- arcee ----

    @Test
    void arceeDefaultUsesDirectTrinityLargeThinkingModelId() {
        var r = registry.resolve(null, ProviderKind.ARCEE);
        assertEquals(ProviderKind.ARCEE, r.resolved().provider());
        assertEquals("trinity-large-thinking", r.resolved().id());
        assertTrue(r.resolved().supportsReasoning());
    }

    @Test
    void arceeTrinityAliasResolvesToDirectLargeThinkingNotOpenrouter() {
        var r = registry.resolve("trinity", ProviderKind.ARCEE);
        assertEquals(ProviderKind.ARCEE, r.resolved().provider());
        assertEquals("trinity-large-thinking", r.resolved().id());
        assertTrue(r.resolved().supportsReasoning());
    }

    @Test
    void arceeTrinityMiniRemainsExplicitCompatibilityModel() {
        var r = registry.resolve("trinity-mini", ProviderKind.ARCEE);
        assertEquals(ProviderKind.ARCEE, r.resolved().provider());
        assertEquals("trinity-mini", r.resolved().id());
        assertFalse(r.resolved().supportsReasoning());
    }

    @Test
    void arceeProviderHintPreservesExplicitFutureModelId() {
        var r = registry.resolve("trinity-large-next", ProviderKind.ARCEE);
        assertEquals(ProviderKind.ARCEE, r.resolved().provider());
        assertEquals("trinity-large-next", r.resolved().id());
        assertFalse(r.resolved().supportsReasoning());
        assertFalse(r.usedFallback());
    }

    // ---- zai ----

    @Test
    void zaiDirectModelsResolveWhenProviderHinted() {
        var defaultResolved = registry.resolve(null, ProviderKind.ZAI);
        assertEquals(ProviderKind.ZAI, defaultResolved.resolved().provider());
        assertEquals("GLM-5.1", defaultResolved.resolved().id());

        var cases = new String[][]{
                {"GLM-5.1", "GLM-5.1"},
                {"glm-5-1", "GLM-5.1"},
                {"GLM-5.2", "GLM-5.2"},
                {"glm-5.2", "GLM-5.2"},
                {"zai-glm-5-2", "GLM-5.2"},
        };
        for (var c : cases) {
            var r = registry.resolve(c[0], ProviderKind.ZAI);
            assertEquals(ProviderKind.ZAI, r.resolved().provider(), "alias: " + c[0]);
            assertEquals(c[1], r.resolved().id(), "alias: " + c[0]);
            assertFalse(r.usedFallback());
            assertTrue(r.resolved().supportsTools());
            assertTrue(r.resolved().supportsReasoning());
        }
    }

    // ---- first-party recent providers ----

    @Test
    void firstPartyRecentProviderModelsAreListed() {
        var models = registry.list();
        assertTrue(models.stream().anyMatch(m -> m.provider() == ProviderKind.ZAI && m.id().equals("GLM-5.2")));
        assertTrue(models.stream().anyMatch(m -> m.provider() == ProviderKind.STEPFUN && m.id().equals("step-3.7-flash")));
        assertTrue(models.stream().anyMatch(m -> m.provider() == ProviderKind.MINIMAX && m.id().equals("MiniMax-M2.1")));
    }

    @Test
    void stepfunAndMinimaxDirectModelsResolveWhenProviderHinted() {
        var stepfun = registry.resolve(null, ProviderKind.STEPFUN);
        assertEquals(ProviderKind.STEPFUN, stepfun.resolved().provider());
        assertEquals("step-3.7-flash", stepfun.resolved().id());

        var minimaxCases = new String[][]{
                {"minimax", "MiniMax-M3"},
                {"minimax-m3", "MiniMax-M3"},
                {"minimax-m2.7", "MiniMax-M2.7"},
                {"minimax-m2-7-highspeed", "MiniMax-M2.7-highspeed"},
                {"minimax-m2.1", "MiniMax-M2.1"},
                {"minimax-m2", "MiniMax-M2"},
        };
        for (var c : minimaxCases) {
            var r = registry.resolve(c[0], ProviderKind.MINIMAX);
            assertEquals(ProviderKind.MINIMAX, r.resolved().provider(), "alias: " + c[0]);
            assertEquals(c[1], r.resolved().id(), "alias: " + c[0]);
            assertFalse(r.usedFallback());
            assertTrue(r.resolved().supportsTools());
            assertTrue(r.resolved().supportsReasoning());
        }
    }

    // ---- casing preservation ----

    @Test
    void preservesRequestedModelCasingForThirdPartyProviders() {
        var r = registry.resolve("DeepSeek-V4-Pro", null);
        assertEquals(ProviderKind.DEEPSEEK, r.resolved().provider());
        assertEquals("DeepSeek-V4-Pro", r.resolved().id());
    }

    @Test
    void registryCasingTakesPriorityOverRequestedCasingWithProviderHint() {
        var r = registry.resolve("DeepSeek-V4-Pro", ProviderKind.DEEPSEEK);
        assertEquals(ProviderKind.DEEPSEEK, r.resolved().provider());
        assertEquals("deepseek-v4-pro", r.resolved().id());
    }

    @Test
    void preservesRequestedModelCasingWithoutSurroundingWhitespace() {
        var r = registry.resolve("  DeepSeek-V4-Pro  ", null);
        assertEquals(ProviderKind.DEEPSEEK, r.resolved().provider());
        assertEquals("DeepSeek-V4-Pro", r.resolved().id());
    }

    @Test
    void aliasMatchDoesNotOverrideRequestedCasing() {
        var r = registry.resolve("deepseek-reasoner", null);
        assertEquals(ProviderKind.DEEPSEEK, r.resolved().provider());
        assertEquals("deepseek-v4-flash", r.resolved().id());
    }

    // ---- model family classification ----

    @Test
    void modelFamilyClassifiesKnownModelIds() {
        assertEquals(ModelFamily.DEEPSEEK, ModelRegistry.modelFamily("deepseek-v4-pro"));
        assertEquals(ModelFamily.OPENAI, ModelRegistry.modelFamily("openai/gpt-5.4"));
        assertEquals(ModelFamily.ANTHROPIC, ModelRegistry.modelFamily("anthropic/claude-opus-4-7"));
        assertEquals(ModelFamily.META, ModelRegistry.modelFamily("meta-llama/llama-3.3-70b-instruct"));
        assertEquals(ModelFamily.QWEN, ModelRegistry.modelFamily("Qwen/Qwen3-Coder"));
    }

    @Test
    void modelFamilyUsesUnderlyingModelForRouterIds() {
        assertEquals(ModelFamily.META, ModelRegistry.modelFamily("groq/llama-3.3-70b-versatile"));
        assertEquals(ModelFamily.OPENAI, ModelRegistry.modelFamily("openrouter/openai/gpt-5.4"));
        assertEquals(ModelFamily.DEEPSEEK, ModelRegistry.modelFamily("fireworks/accounts/fireworks/models/deepseek-v4-pro"));
    }

    @Test
    void modelFamilyCoversProminentGoogleAndMistralModelNames() {
        assertEquals(ModelFamily.GOOGLE, ModelRegistry.modelFamily("google/gemma-3-27b-it"));
        assertEquals(ModelFamily.MISTRAL, ModelRegistry.modelFamily("mistralai/mixtral-8x22b"));
        assertEquals(ModelFamily.MISTRAL, ModelRegistry.modelFamily("codestral-latest"));
    }

    @Test
    void modelFamilyFallsBackToInferencerForUnknownModels() {
        assertEquals(ModelFamily.INFERENCER, ModelRegistry.modelFamily("custom-gateway/my-private-model"));
        assertEquals(ModelFamily.INFERENCER, ModelRegistry.modelFamily(""));
    }

    // ---- fallback chain ----

    @Test
    void fallbackChainTracksResolutionSteps() {
        var r = registry.resolve("deepseek-v4-pro", null);
        assertFalse(r.fallbackChain().isEmpty());
        assertTrue(r.fallbackChain().get(0).startsWith("requested:"));
    }

    @Test
    void providerDefaultAddsToFallbackChain() {
        var r = registry.resolve("nonexistent-model-xyz", ProviderKind.DEEPSEEK);
        assertTrue(r.usedFallback());
        assertTrue(r.fallbackChain().stream().anyMatch(s -> s.startsWith("provider_default:")));
    }

    @Test
    void listReturnsAllModels() {
        var models = registry.list();
        assertFalse(models.isEmpty());
        assertTrue(models.size() >= 60);
    }
}
