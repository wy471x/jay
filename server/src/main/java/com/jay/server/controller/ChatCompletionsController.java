package com.jay.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jay.agent.ModelInfo;
import com.jay.agent.ModelRegistry;
import com.jay.agent.ProviderKind;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * OpenAI-compatible /v1/chat/completions pass-through proxy.
 * Resolves the target provider from the model registry and forwards upstream.
 */
@RestController
public class ChatCompletionsController {

    private static final ObjectMapper mapper = new ObjectMapper();

    /** Maps provider to default base URL. Real impl loads from config. */
    private static final Map<ProviderKind, String> PROVIDER_BASE_URLS = Map.of(
        ProviderKind.ANTHROPIC, "https://api.anthropic.com",
        ProviderKind.OPENAI, "https://api.openai.com",
        ProviderKind.DEEPSEEK, "https://api.deepseek.com",
        ProviderKind.NVIDIA_NIM, "https://integrate.api.nvidia.com",
        ProviderKind.OPENROUTER, "https://openrouter.ai/api"
    );

    private final RestClient restClient;
    private final ModelRegistry modelRegistry;

    public ChatCompletionsController(RestClient restClient, ModelRegistry modelRegistry) {
        this.restClient = restClient;
        this.modelRegistry = modelRegistry;
    }

    @PostMapping("/v1/chat/completions")
    public ResponseEntity<String> chatCompletions(@RequestBody JsonNode body,
                                                   @RequestHeader(value = "Authorization", required = false)
                                                   String authHeader) {
        // Reject streaming
        if (body.has("stream") && body.get("stream").asBoolean(false)) {
            return ResponseEntity.status(400).body(
                "{\"error\":{\"message\":\"Streaming is not supported\",\"code\":\"streaming_unsupported\"}}");
        }

        // Resolve model
        String modelName = body.has("model") ? body.get("model").asText() : null;
        var resolution = modelRegistry.resolve(modelName, null);
        ModelInfo info = resolution.resolved();
        if (info == null) {
            return ResponseEntity.status(400).body(
                "{\"error\":{\"message\":\"Unknown model: " + modelName +
                "\",\"code\":\"unknown_model\"}}");
        }

        // Build upstream URL from provider's base URL
        String baseUrl = PROVIDER_BASE_URLS.get(info.provider());
        if (baseUrl == null) {
            return ResponseEntity.status(400).body(
                "{\"error\":{\"message\":\"No base URL for provider: " + info.provider() +
                "\",\"code\":\"no_base_url\"}}");
        }
        String upstreamUrl = buildUpstreamUrl(baseUrl);

        // Inject model ID if missing
        if (!body.has("model") || body.get("model").asText().isBlank()) {
            ObjectNode mutable = body.deepCopy();
            mutable.put("model", info.id());
            body = mutable;
        }

        // Build upstream request — API key from config (placeholder: delegate to incoming)
        var upstream = restClient.post()
            .uri(upstreamUrl)
            .contentType(MediaType.APPLICATION_JSON);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            upstream = upstream.header("Authorization", authHeader);
        }

        try {
            ResponseEntity<String> upstreamResp = upstream.body(body.toString()).retrieve()
                .toEntity(String.class);
            return ResponseEntity.status(upstreamResp.getStatusCode())
                .headers(h -> h.putAll(upstreamResp.getHeaders()))
                .body(upstreamResp.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(502).body(
                "{\"error\":{\"message\":\"Upstream error: " + escapeJson(e.getMessage()) +
                "\",\"code\":\"bad_gateway\"}}");
        }
    }

    String buildUpstreamUrl(String baseUrl) {
        String cleaned = baseUrl.replaceAll("/+$", "");
        if (cleaned.endsWith("/beta")) cleaned = cleaned.substring(0, cleaned.length() - 5);
        cleaned = cleaned.replaceAll("/v\\d+$", "");
        return cleaned + "/v1/chat/completions";
    }

    private static String escapeJson(String s) {
        return s == null ? "null" : s.replace("\"", "\\\"");
    }
}
