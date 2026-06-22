package com.jay.tui.client;

import java.util.List;
import java.util.concurrent.Flow;

/**
 * Interface for LLM API clients (OpenAI-compatible Chat Completions).
 *
 * <p>Equivalent to Rust's LlmClient trait.
 */
public interface LlmClient {

    /**
     * Send a chat completion request and receive a reactive stream of deltas.
     * The returned publisher emits StreamEvents as SSE chunks arrive.
     */
    Flow.Publisher<StreamEvent> chat(String model, List<ChatMessage> messages,
                                     double temperature, int maxTokens);

    /**
     * Convenience: collect all stream events into a blocking result.
     */
    default ChatResult chatBlocking(String model, List<ChatMessage> messages,
                                     double temperature, int maxTokens) {
        var publisher = chat(model, messages, temperature, maxTokens);
        var collector = new StreamCollector();
        publisher.subscribe(collector);
        return collector.await();
    }

    /** Result of a completed chat request. */
    record ChatResult(String content, List<ChatMessage.ToolCall> toolCalls,
                      StreamEvent.Usage usage, boolean success, String error) {
        public static ChatResult success(String content, List<ChatMessage.ToolCall> toolCalls,
                                          StreamEvent.Usage usage) {
            return new ChatResult(content, toolCalls, usage, true, null);
        }

        public static ChatResult error(String error) {
            return new ChatResult(null, List.of(), null, false, error);
        }
    }
}
