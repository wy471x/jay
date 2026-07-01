package com.jay.tui.core.turn;

import com.jay.tui.client.ChatMessage;
import com.jay.tui.client.ContentBlock;
import com.jay.tui.core.StreamState;
import com.jay.tui.core.TuiEngineEvent;
import com.jay.tui.core.TurnContext;
import com.jay.tui.core.compaction.CompactionConfig;
import com.jay.tui.core.compaction.CompactionExecutor;
import com.jay.tui.core.compaction.CompactionPlanner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Orchestrates a single LLM turn: compaction check → request build →
 * SSE streaming → tool execution → loop.
 *
 * <p>Mirrors Rust {@code handle_deepseek_turn()} in {@code turn_loop.rs}.
 * Designed to be called from the Engine's virtual thread.
 */
public class SseTurnLoop {

    // Limits matching Rust turn_loop.rs constants
    private static final int MAX_STREAM_RETRIES = 3;
    private static final int MAX_TRANSPARENT_STREAM_RETRIES = 2;
    private static final int MAX_TOOL_RESULT_CHARS = 12_000;
    private static final int TOOL_RESULT_HEAD_CHARS = 4_000;
    private static final int TOOL_RESULT_TAIL_CHARS = 4_000;

    private final BlockingQueue<TuiEngineEvent> eventQueue;
    private final AtomicBoolean cancelToken;
    private final CompactionPlanner compactionPlanner;
    private final ToolDispatcher toolDispatcher;
    private final List<ChatMessage> sessionMessages;

    public SseTurnLoop(BlockingQueue<TuiEngineEvent> eventQueue,
                        AtomicBoolean cancelToken,
                        CompactionConfig compactionConfig,
                        List<ChatMessage> sessionMessages) {
        this.eventQueue = eventQueue;
        this.cancelToken = cancelToken;
        this.compactionPlanner = new CompactionPlanner(compactionConfig);
        this.toolDispatcher = new ToolDispatcher(eventQueue);
        this.sessionMessages = sessionMessages;
    }

    /**
     * Execute a turn: send the user message, stream the response,
     * execute any tool calls, and loop until no more tool calls
     * or max steps reached.
     *
     * @return the number of steps taken
     */
    public int executeTurn(TurnContext turn, String userMessage) {
        // Step 0: Check if compaction is needed
        checkCompaction();

        // Add user message to session
        sessionMessages.add(ChatMessage.user(userMessage));

        // Main turn loop
        while (!turn.atMaxSteps() && !cancelToken.get()) {
            // 1. Build content blocks for this step
            List<ContentBlock> contentBlocks = requestStream(turn);

            if (cancelToken.get()) {
                emit(new TuiEngineEvent.TurnAborted(turn.turnId(), "cancelled"));
                return turn.step();
            }

            // 2. Separate text from tool calls
            List<ContentBlock.Text> textBlocks = new ArrayList<>();
            List<ContentBlock.ToolUse> toolUses = new ArrayList<>();

            for (var block : contentBlocks) {
                switch (block) {
                    case ContentBlock.Text t -> textBlocks.add(t);
                    case ContentBlock.ToolUse tu -> toolUses.add(tu);
                    case ContentBlock.Thinking th -> {} // thinking blocks not stored separately
                    default -> {}
                }
            }

            // 3. Add assistant message to session
            if (!textBlocks.isEmpty()) {
                String combined = String.join("",
                        textBlocks.stream().map(ContentBlock.Text::content).toList());
                sessionMessages.add(ChatMessage.assistant(combined));
            }

            // 4. Execute tool calls if any
            if (!toolUses.isEmpty()) {
                turn.recordToolCall();

                // Plan batches
                List<ToolDispatcher.ToolBatch> batches = toolDispatcher.planBatches(toolUses);

                // Execute each batch
                List<TuiEngineEvent.ToolResult> results = new ArrayList<>();
                for (var batch : batches) {
                    if (cancelToken.get()) break;
                    results.addAll(toolDispatcher.executeBatch(batch));
                }

                // Add tool results to session
                for (int i = 0; i < toolUses.size() && i < results.size(); i++) {
                    var tu = toolUses.get(i);
                    var result = results.get(i);
                    String content = result.success()
                            ? compactToolResult(result.content())
                            : "Error: " + result.error();
                    sessionMessages.add(ChatMessage.tool(tu.id(), content));
                }

                // Loop back for next step
                turn.nextStep();
                if (!turn.atMaxSteps()) {
                    // Request another streaming response with tool results
                    // This would re-enter the LLM with updated messages
                    continue;
                }
            }

            // 5. No more tool calls — turn complete
            break;
        }

        emit(new TuiEngineEvent.TurnComplete(
                turn.turnId(),
                (int) turn.inputTokens(),
                (int) turn.outputTokens(),
                cancelToken.get()
                        ? TuiEngineEvent.TurnOutcomeStatus.INTERRUPTED
                        : TuiEngineEvent.TurnOutcomeStatus.COMPLETED,
                null
        ));

        return turn.step();
    }

    /**
     * Request and process an SSE stream from the LLM.
     * Returns the parsed content blocks.
     *
     * <p>In the full implementation (Phase 3+), this connects to
     * {@code StreamingSseClient}. For now, uses the existing
     * {@code OpenAiClient} with content-block tracking.
     */
    private List<ContentBlock> requestStream(TurnContext turn) {
        var streamState = new StreamState();
        int transparentRetries = 0;

        while (transparentRetries <= MAX_TRANSPARENT_STREAM_RETRIES && !cancelToken.get()) {
            try {
                // The real implementation will:
                // 1. Build MessageRequest from session messages
                // 2. Call StreamingSseClient.createMessageStream()
                // 3. Process SSE events into ContentBlocks via StreamState

                // Placeholder: simulate a text response
                emit(new TuiEngineEvent.MessageStarted(0, "text"));
                String response = "[SSE response — Phase 3 streaming client integration pending]";
                emit(new TuiEngineEvent.MessageDelta(0, response));
                emit(new TuiEngineEvent.MessageComplete(0, "text"));

                turn.addUsage(50, 20);
                return List.of(ContentBlock.text(response));

            } catch (Exception e) {
                // Transparent retry: if no content received, retry up to limit
                if (!streamState.anyContentReceived()
                        && transparentRetries < MAX_TRANSPARENT_STREAM_RETRIES) {
                    transparentRetries++;
                    streamState.reset();
                    continue;
                }
                emit(new TuiEngineEvent.EngineError(
                        e.getMessage(), "stream", transparentRetries == 0));
                return List.of();
            }
        }
        return List.of(ContentBlock.text("Stream terminated"));
    }

    // ── Compaction ──────────────────────────────────────────────────────

    private void checkCompaction() {
        if (compactionPlanner.shouldCompact(sessionMessages)) {
            var plan = compactionPlanner.planCompaction(sessionMessages);
            emit(new TuiEngineEvent.CompactionStarted(
                    "auto", true, plan.description()));

            // Build the executor and run safe compaction
            // Note: full LLM-based compaction requires an LlmClient.
            // For now, emit the plan and do local tool-result pruning as a quick win.
            int before = sessionMessages.size();
            boolean localOnly = true; // set to false when LlmClient is wired in

            if (localOnly) {
                // Local padding: truncate tool results in the summarize range
                for (int idx : plan.summarizeIndices()) {
                    if (idx < sessionMessages.size()) {
                        var msg = sessionMessages.get(idx);
                        if ("tool".equals(msg.role()) && msg.content() != null
                                && msg.content().length() > MAX_TOOL_RESULT_CHARS) {
                            String truncated = com.jay.tui.core.compaction.CompactionExecutor
                                    .headTailTruncate(msg.content(),
                                            TOOL_RESULT_HEAD_CHARS, TOOL_RESULT_TAIL_CHARS);
                            sessionMessages.set(idx,
                                    com.jay.tui.client.ChatMessage.tool(msg.toolCallId(), truncated));
                        }
                    }
                }
            }

            emit(new TuiEngineEvent.CompactionCompleted(
                    "auto", true, plan.description(),
                    before, sessionMessages.size()));
        }
    }

    /** Truncate tool result to fit within context budget. */
    static String compactToolResult(String content) {
        if (content == null || content.length() <= MAX_TOOL_RESULT_CHARS) {
            return content;
        }
        return com.jay.tui.core.compaction.CompactionExecutor.headTailTruncate(
                content, TOOL_RESULT_HEAD_CHARS, TOOL_RESULT_TAIL_CHARS);
    }

    private void emit(TuiEngineEvent event) {
        eventQueue.offer(event);
    }
}
