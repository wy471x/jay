package com.jay.tui.client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

/**
 * Collects SSE stream events into a final ChatResult.
 * Implements Flow.Subscriber for reactive consumption.
 *
 * <p>Equivalent to Rust's stream collection / append_to_buffer logic.
 */
public class StreamCollector implements Flow.Subscriber<StreamEvent> {

    private final StringBuilder content = new StringBuilder();
    private final StringBuilder reasoning = new StringBuilder();
    private final List<ChatMessage.ToolCall> toolCalls = new ArrayList<>();
    private StreamEvent.Usage usage;
    private String error;
    private final CountDownLatch done = new CountDownLatch(1);
    private Flow.Subscription subscription;
    private volatile boolean completed;

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(StreamEvent event) {
        switch (event) {
            case StreamEvent.ContentDelta d -> {
                if (d.content() != null) content.append(d.content());
                if (d.reasoning() != null) reasoning.append(d.reasoning());
            }
            case StreamEvent.ToolCallDelta d -> {
                toolCalls.add(new ChatMessage.ToolCall(
                        d.id(), "function",
                        new ChatMessage.ToolCall.Function(d.name(), d.arguments())));
            }
            case StreamEvent.Done d -> { /* stop reason logged */ }
            case StreamEvent.Usage u -> usage = u;
            case StreamEvent.Error e -> error = e.message();
        }
    }

    @Override
    public void onError(Throwable t) {
        error = t.getMessage();
        completed = true;
        done.countDown();
    }

    @Override
    public void onComplete() {
        completed = true;
        done.countDown();
    }

    /** Block until the stream completes, then return the result. */
    public LlmClient.ChatResult await() {
        try {
            done.await(120, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return LlmClient.ChatResult.error("interrupted");
        }
        if (error != null) {
            return LlmClient.ChatResult.error(error);
        }
        return LlmClient.ChatResult.success(content.toString(), toolCalls, usage);
    }

    public String currentContent() {
        return content.toString();
    }

    /** Get intermediate content + reasoning for rendering while streaming. */
    public String displayContent() {
        var sb = new StringBuilder();
        if (!reasoning.isEmpty()) {
            sb.append("<thinking>\n").append(reasoning).append("\n</thinking>\n\n");
        }
        sb.append(content);
        return sb.toString();
    }

    public boolean isCompleted() {
        return completed;
    }
}
