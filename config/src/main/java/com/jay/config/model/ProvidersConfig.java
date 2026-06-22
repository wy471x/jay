package com.jay.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jay.agent.ProviderKind;

/**
 * Container for all 25 provider config blocks. Equivalent to Rust's ProvidersToml.
 */
public class ProvidersConfig {

    @JsonProperty("deepseek")      private ProviderConfigToml deepseek;
    @JsonProperty("nvidia-nim")    private ProviderConfigToml nvidiaNim;
    @JsonProperty("openai")        private ProviderConfigToml openai;
    @JsonProperty("atlascloud")    private ProviderConfigToml atlascloud;
    @JsonProperty("wanjie-ark")    private ProviderConfigToml wanjieArk;
    @JsonProperty("volcengine")    private ProviderConfigToml volcengine;
    @JsonProperty("openrouter")    private ProviderConfigToml openrouter;
    @JsonProperty("xiaomi-mimo")   private ProviderConfigToml xiaomiMimo;
    @JsonProperty("novita")        private ProviderConfigToml novita;
    @JsonProperty("fireworks")     private ProviderConfigToml fireworks;
    @JsonProperty("siliconflow")   private ProviderConfigToml siliconflow;
    @JsonProperty("arcee")         private ProviderConfigToml arcee;
    @JsonProperty("siliconflow-cn") private ProviderConfigToml siliconflowCN;
    @JsonProperty("moonshot")      private ProviderConfigToml moonshot;
    @JsonProperty("sglang")        private ProviderConfigToml sglang;
    @JsonProperty("vllm")          private ProviderConfigToml vllm;
    @JsonProperty("ollama")        private ProviderConfigToml ollama;
    @JsonProperty("huggingface")   private ProviderConfigToml huggingface;
    @JsonProperty("together")      private ProviderConfigToml together;
    @JsonProperty("openai-codex")  private ProviderConfigToml openaiCodex;
    @JsonProperty("anthropic")     private ProviderConfigToml anthropic;
    @JsonProperty("z-ai")          private ProviderConfigToml zai;
    @JsonProperty("stepfun")       private ProviderConfigToml stepfun;
    @JsonProperty("minimax")       private ProviderConfigToml minimax;
    @JsonProperty("deepinfra")     private ProviderConfigToml deepinfra;

    /** Look up a provider's config block by kind. */
    public ProviderConfigToml forProvider(ProviderKind kind) {
        return switch (kind) {
            case DEEPSEEK -> deepseek;
            case NVIDIA_NIM -> nvidiaNim;
            case OPENAI -> openai;
            case ATLASCLOUD -> atlascloud;
            case WANJIE_ARK -> wanjieArk;
            case VOLCENGINE -> volcengine;
            case OPENROUTER -> openrouter;
            case XIAOMI_MIMO -> xiaomiMimo;
            case NOVITA -> novita;
            case FIREWORKS -> fireworks;
            case SILICONFLOW -> siliconflow;
            case ARCEE -> arcee;
            case SILICONFLOW_CN -> siliconflowCN;
            case MOONSHOT -> moonshot;
            case SGLANG -> sglang;
            case VLLM -> vllm;
            case OLLAMA -> ollama;
            case HUGGINGFACE -> huggingface;
            case TOGETHER -> together;
            case OPENAI_CODEX -> openaiCodex;
            case ANTHROPIC -> anthropic;
            case ZAI -> zai;
            case STEPFUN -> stepfun;
            case MINIMAX -> minimax;
            case DEEPINFRA -> deepinfra;
        };
    }

    /** Merge only non-sensitive fields (model) from project overrides. */
    void mergeNonSensitive(ProvidersConfig other) {
        for (ProviderKind kind : ProviderKind.values()) {
            ProviderConfigToml src = other.forProvider(kind);
            ProviderConfigToml dst = forProvider(kind);
            if (src != null && dst != null && src.model() != null) {
                dst.model(src.model());
            }
        }
    }

    // Getters/setters for Jackson
    public ProviderConfigToml deepseek() { return deepseek; }

    public void deepseek(ProviderConfigToml v) { deepseek = v; }

    public ProviderConfigToml nvidiaNim() { return nvidiaNim; }

    public void nvidiaNim(ProviderConfigToml v) { nvidiaNim = v; }

    public ProviderConfigToml openai() { return openai; }

    public void openai(ProviderConfigToml v) { openai = v; }

    public ProviderConfigToml atlascloud() { return atlascloud; }

    public void atlascloud(ProviderConfigToml v) { atlascloud = v; }

    public ProviderConfigToml wanjieArk() { return wanjieArk; }

    public void wanjieArk(ProviderConfigToml v) { wanjieArk = v; }

    public ProviderConfigToml volcengine() { return volcengine; }

    public void volcengine(ProviderConfigToml v) { volcengine = v; }

    public ProviderConfigToml openrouter() { return openrouter; }

    public void openrouter(ProviderConfigToml v) { openrouter = v; }

    public ProviderConfigToml xiaomiMimo() { return xiaomiMimo; }

    public void xiaomiMimo(ProviderConfigToml v) { xiaomiMimo = v; }

    public ProviderConfigToml novita() { return novita; }

    public void novita(ProviderConfigToml v) { novita = v; }

    public ProviderConfigToml fireworks() { return fireworks; }

    public void fireworks(ProviderConfigToml v) { fireworks = v; }

    public ProviderConfigToml siliconflow() { return siliconflow; }

    public void siliconflow(ProviderConfigToml v) { siliconflow = v; }

    public ProviderConfigToml arcee() { return arcee; }

    public void arcee(ProviderConfigToml v) { arcee = v; }

    public ProviderConfigToml siliconflowCN() { return siliconflowCN; }

    public void siliconflowCN(ProviderConfigToml v) { siliconflowCN = v; }

    public ProviderConfigToml moonshot() { return moonshot; }

    public void moonshot(ProviderConfigToml v) { moonshot = v; }

    public ProviderConfigToml sglang() { return sglang; }

    public void sglang(ProviderConfigToml v) { sglang = v; }

    public ProviderConfigToml vllm() { return vllm; }

    public void vllm(ProviderConfigToml v) { vllm = v; }

    public ProviderConfigToml ollama() { return ollama; }

    public void ollama(ProviderConfigToml v) { ollama = v; }

    public ProviderConfigToml huggingface() { return huggingface; }

    public void huggingface(ProviderConfigToml v) { huggingface = v; }

    public ProviderConfigToml together() { return together; }

    public void together(ProviderConfigToml v) { together = v; }

    public ProviderConfigToml openaiCodex() { return openaiCodex; }

    public void openaiCodex(ProviderConfigToml v) { openaiCodex = v; }

    public ProviderConfigToml anthropic() { return anthropic; }

    public void anthropic(ProviderConfigToml v) { anthropic = v; }

    public ProviderConfigToml zai() { return zai; }

    public void zai(ProviderConfigToml v) { zai = v; }

    public ProviderConfigToml stepfun() { return stepfun; }

    public void stepfun(ProviderConfigToml v) { stepfun = v; }

    public ProviderConfigToml minimax() { return minimax; }

    public void minimax(ProviderConfigToml v) { minimax = v; }

    public ProviderConfigToml deepinfra() { return deepinfra; }

    public void deepinfra(ProviderConfigToml v) { deepinfra = v; }
}
