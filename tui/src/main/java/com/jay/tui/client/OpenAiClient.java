package com.jay.tui.client;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

/**
 * OpenAI-compatible Chat Completions client with SSE streaming.
 *
 * <p>Sends POST to {baseUrl}/v1/chat/completions with stream:true,
 * parses SSE "data:" lines into StreamEvent deltas.
 *
 * <p>Equivalent to Rust's client.rs / AnthropicClient.
 */
public class OpenAiClient implements LlmClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Duration TIMEOUT = Duration.ofSeconds(120);

    private final HttpClient httpClient;
    private final String baseUrl;
    private final String apiKey;

    public OpenAiClient(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public Flow.Publisher<StreamEvent> chat(String model, List<ChatMessage> messages,
                                             double temperature, int maxTokens) {
        var publisher = new SubmissionPublisher<StreamEvent>();
        Thread.ofVirtual().name("llm-sse").start(() -> {
            try {
                streamRequest(model, messages, temperature, maxTokens, publisher);
            } catch (Exception e) {
                publisher.submit(new StreamEvent.Error(e.getMessage()));
            } finally {
                publisher.close();
            }
        });
        return publisher;
    }

    private void streamRequest(String model, List<ChatMessage> messages,
                                double temperature, int maxTokens,
                                SubmissionPublisher<StreamEvent> publisher) throws Exception {
        var body = MAPPER.createObjectNode();
        body.put("model", model);
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);
        body.put("stream", true);

        var msgs = body.putArray("messages");
        for (var msg : messages) {
            var m = msgs.addObject();
            m.put("role", msg.role());
            m.put("content", msg.content());
        }

        var request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/chat/completions"))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
                .build();

        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            publisher.submit(new StreamEvent.Error(
                    "HTTP " + response.statusCode() + ": " +
                            new String(response.body().readAllBytes())));
            return;
        }

        try (var reader = new BufferedReader(new InputStreamReader(response.body()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data: ")) {
                    String data = line.substring(6).trim();
                    if ("[DONE]".equals(data)) {
                        publisher.submit(new StreamEvent.Done("stop"));
                        continue;
                    }
                    try {
                        parseDelta(data, publisher);
                    } catch (Exception e) {
                        // skip malformed lines
                    }
                }
            }
        }
    }

    private void parseDelta(String json, SubmissionPublisher<StreamEvent> publisher)
            throws Exception {
        var root = MAPPER.readTree(json);
        var choices = root.path("choices");
        if (choices.isEmpty()) return;

        var delta = choices.get(0).path("delta");
        if (delta.isMissingNode()) return;

        // Content delta
        if (delta.has("content")) {
            String content = delta.get("content").asText();
            String reasoning = delta.has("reasoning_content")
                    ? delta.get("reasoning_content").asText() : null;
            if (!content.isEmpty() || reasoning != null) {
                publisher.submit(new StreamEvent.ContentDelta(content, reasoning));
            }
        }

        // Tool call delta
        if (delta.has("tool_calls")) {
            for (var tc : delta.get("tool_calls")) {
                String id = tc.has("id") ? tc.get("id").asText() : "";
                var fn = tc.path("function");
                String name = fn.has("name") ? fn.get("name").asText() : "";
                String args = fn.has("arguments") ? fn.get("arguments").asText() : "";
                publisher.submit(new StreamEvent.ToolCallDelta(id, name, args));
            }
        }

        // Usage
        if (root.has("usage")) {
            var usage = root.get("usage");
            int prompt = usage.has("prompt_tokens") ? usage.get("prompt_tokens").asInt() : 0;
            int completion = usage.has("completion_tokens")
                    ? usage.get("completion_tokens").asInt() : 0;
            publisher.submit(new StreamEvent.Usage(prompt, completion));
        }
    }
}
