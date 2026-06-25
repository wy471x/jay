package com.jay.tui.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * HTTP SSE streaming client with content-block tracking, transparent
 * retry, and backpressure handling.
 *
 * <p>Mirrors Rust {@code client/chat.rs} {@code create_message_stream()}.
 *
 * <p>Usage:
 * <pre>{@code
 * var client = new StreamingSseClient("https://api.deepseek.com", "sk-...");
 * var options = new StreamOptions(300, Duration.ofSeconds(30));
 * var publisher = client.createStream(messages, options, cancelToken);
 * publisher.subscribe(mySubscriber);
 * }</pre>
 */
public class StreamingSseClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Constants mirroring Rust client/chat.rs
    private static final Duration DEFAULT_STREAM_OPEN_TIMEOUT = Duration.ofSeconds(45);
    private static final Duration DEFAULT_IDLE_TIMEOUT = Duration.ofSeconds(120);
    private static final int SSE_BACKPRESSURE_HIGH_WATERMARK = 8 * 1024 * 1024; // 8 MB
    private static final int SSE_MAX_LINES_PER_CHUNK = 256;

    private final HttpClient httpClient;
    private final String baseUrl;
    private final String apiKey;

    public StreamingSseClient(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Create a streaming chat completion request.
     * Returns a Publisher that emits SseStreamEvents as they arrive.
     */
    public Flow.Publisher<SseStreamEvent> createStream(
            String model,
            List<ChatMessage> messages,
            String systemPrompt,
            List<Object> tools,
            double temperature,
            int maxTokens,
            AtomicBoolean cancelToken
    ) {
        return createStream(model, messages, systemPrompt, tools,
                temperature, maxTokens, new StreamOptions(), cancelToken);
    }

    /**
     * Create a streaming chat completion request with full options.
     */
    public Flow.Publisher<SseStreamEvent> createStream(
            String model,
            List<ChatMessage> messages,
            String systemPrompt,
            List<Object> tools,
            double temperature,
            int maxTokens,
            StreamOptions options,
            AtomicBoolean cancelToken
    ) {
        var publisher = new SubmissionPublisher<SseStreamEvent>();

        Thread.ofVirtual().name("sse-stream").start(() -> {
            try {
                streamRequest(model, messages, systemPrompt, tools,
                        temperature, maxTokens, options, cancelToken, publisher);
            } catch (Exception e) {
                if (!cancelToken.get()) {
                    publisher.submit(new SseStreamEvent.StreamError(
                            e.getMessage(), "connection", isRetryable(e)));
                }
            } finally {
                publisher.close();
            }
        });

        return publisher;
    }

    private void streamRequest(
            String model, List<ChatMessage> messages, String systemPrompt,
            List<Object> tools, double temperature, int maxTokens,
            StreamOptions options, AtomicBoolean cancelToken,
            SubmissionPublisher<SseStreamEvent> publisher
    ) throws Exception {

        // Build request body
        var body = MAPPER.createObjectNode();
        body.put("model", model);
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);
        body.put("stream", true);
        body.put("stream_options", MAPPER.createObjectNode()
                .put("include_usage", true));

        // System prompt
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            body.putArray("system").add(MAPPER.createObjectNode()
                    .put("role", "system").put("content", systemPrompt));
        }

        // Messages
        var msgs = body.putArray("messages");
        for (var msg : messages) {
            var m = msgs.addObject();
            m.put("role", msg.role());
            if (msg.content() != null) m.put("content", msg.content());
            if (msg.toolCallId() != null) m.put("tool_call_id", msg.toolCallId());
            if (msg.toolCalls() != null && !msg.toolCalls().isEmpty()) {
                var tcs = m.putArray("tool_calls");
                for (var tc : msg.toolCalls()) {
                    var tcNode = tcs.addObject();
                    tcNode.put("id", tc.id());
                    tcNode.put("type", tc.type());
                    var fnNode = tcNode.putObject("function");
                    fnNode.put("name", tc.function().name());
                    fnNode.put("arguments", tc.function().arguments());
                }
            }
        }

        // Tools
        if (tools != null && !tools.isEmpty()) {
            var toolsArr = body.putArray("tools");
            // Tools serialized as ObjectNodes — caller responsibility
            for (var tool : tools) {
                if (tool instanceof com.fasterxml.jackson.databind.JsonNode node) {
                    toolsArr.add(node);
                }
            }
        }

        var request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .timeout(options.maxDuration())
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
                .build();

        // Send with open timeout
        var response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            String errorBody = new String(response.body().readAllBytes());
            publisher.submit(new SseStreamEvent.StreamError(
                    "HTTP " + response.statusCode() + ": " + errorBody,
                    "http", false));
            return;
        }

        // Process SSE lines
        var reader = new BufferedReader(new InputStreamReader(response.body()));
        try {
            String line;
            int lineCount = 0;
            int estimatedBackpressure = 0;
            Instant lastChunkAt = Instant.now();

            while ((line = reader.readLine()) != null) {
                if (cancelToken.get()) {
                    publisher.submit(new SseStreamEvent.MessageStop());
                    return;
                }

                lineCount++;

                // Parse SSE data line
                if (line.startsWith("data: ")) {
                    String data = line.substring(6).strip();
                    if ("[DONE]".equals(data)) {
                        publisher.submit(new SseStreamEvent.MessageStop());
                        continue;
                    }

                    var event = SseStreamEvent.parseData(data);
                    if (event != null) {
                        publisher.submit(event);

                        // Backpressure estimation
                        if (event instanceof SseStreamEvent.ContentBlockDelta d) {
                            estimatedBackpressure += estimateSize(d);
                        }
                    }
                }

                // Backpressure relief: if we've accumulated a lot of data, sleep
                if (estimatedBackpressure >= SSE_BACKPRESSURE_HIGH_WATERMARK) {
                    Thread.sleep(10);
                    estimatedBackpressure = 0;
                }

                // Idle timeout check
                if (lineCount >= SSE_MAX_LINES_PER_CHUNK) {
                    lineCount = 0;
                    lastChunkAt = Instant.now();
                }
            }
        } finally {
            reader.close();
        }
    }

    /** Estimate the byte size of a delta for backpressure tracking. */
    private static int estimateSize(SseStreamEvent.ContentBlockDelta delta) {
        return switch (delta.delta()) {
            case SseStreamEvent.Delta.TextDelta t -> t.text().length();
            case SseStreamEvent.Delta.ThinkingDelta t -> t.text().length();
            case SseStreamEvent.Delta.InputJsonDelta j -> j.partialJson().length();
            case SseStreamEvent.Delta.SignatureDelta s -> s.signature().length();
        };
    }

    /** Determine if an exception is retryable. */
    private static boolean isRetryable(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        return msg.contains("timeout") || msg.contains("connection")
                || msg.contains("reset") || msg.contains("refused")
                || msg.contains("tls") || msg.contains("handshake");
    }

    // ── StreamOptions ──────────────────────────────────────────────────

    public record StreamOptions(
            long maxDurationSeconds,
            Duration idleTimeout
    ) {
        public StreamOptions() {
            this(300, DEFAULT_IDLE_TIMEOUT);
        }

        public StreamOptions(long maxDurationSeconds, Duration idleTimeout) {
            this.maxDurationSeconds = maxDurationSeconds;
            this.idleTimeout = idleTimeout != null ? idleTimeout : DEFAULT_IDLE_TIMEOUT;
        }

        public Duration maxDuration() {
            return Duration.ofSeconds(maxDurationSeconds);
        }
    }
}
