package com.jay.tui.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Thread-safe handle for the render thread to interact with the engine.
 * Mirrors Rust {@code EngineHandle}.
 *
 * <p>Channel architecture:
 * <ul>
 *   <li>{@code opQueue} — render thread writes ops, engine virtual thread reads</li>
 *   <li>{@code eventQueue} — engine virtual thread writes events, render thread reads</li>
 *   <li>{@code cancelRequested} — shared cancellation flag</li>
 * </ul>
 */
public class EngineHandle {

    private static final int DEFAULT_CAPACITY = 256;

    private final BlockingQueue<TuiEngineOp> opQueue;
    private final BlockingQueue<TuiEngineEvent> eventQueue;
    private final AtomicBoolean cancelRequested;
    private final AtomicBoolean running;
    private volatile Thread engineThread;

    public EngineHandle() {
        this(DEFAULT_CAPACITY);
    }

    public EngineHandle(int capacity) {
        this.opQueue = new LinkedBlockingQueue<>(capacity);
        this.eventQueue = new LinkedBlockingQueue<>(capacity);
        this.cancelRequested = new AtomicBoolean(false);
        this.running = new AtomicBoolean(false);
    }

    // ── Package-private accessors for Engine ──────────────────────────

    BlockingQueue<TuiEngineOp> opQueue() { return opQueue; }
    BlockingQueue<TuiEngineEvent> eventQueue() { return eventQueue; }
    AtomicBoolean cancelRequested() { return cancelRequested; }

    void markRunning(Thread thread) {
        this.engineThread = thread;
        running.set(true);
    }

    void markStopped() {
        running.set(false);
    }

    // ── Public API for render thread ──────────────────────────────────

    /** Enqueue an operation. Non-blocking — returns false if queue is full. */
    public boolean sendOp(TuiEngineOp op) {
        return opQueue.offer(op);
    }

    /** Enqueue an operation, blocking up to the given timeout if queue is full. */
    public boolean sendOp(TuiEngineOp op, long timeoutMs) throws InterruptedException {
        return opQueue.offer(op, timeoutMs, TimeUnit.MILLISECONDS);
    }

    /** Drain all available events. Called each render tick. Non-blocking. */
    public List<TuiEngineEvent> drainEvents() {
        var events = new ArrayList<TuiEngineEvent>();
        eventQueue.drainTo(events);
        return events;
    }

    /** Poll for a single event with timeout. */
    public TuiEngineEvent pollEvent(long timeoutMs) throws InterruptedException {
        return eventQueue.poll(timeoutMs, TimeUnit.MILLISECONDS);
    }

    /** Request cancellation of the current operation. */
    public void cancel() {
        cancelRequested.set(true);
    }

    /** Reset the cancellation flag (after handling). */
    public void resetCancel() {
        cancelRequested.set(false);
    }

    /** Whether cancellation has been requested. */
    public boolean isCancelled() {
        return cancelRequested.get();
    }

    /** Whether the engine thread is still running. */
    public boolean isRunning() {
        return running.get();
    }

    /** Request graceful shutdown and wait for the engine thread to exit. */
    public void shutdown() {
        sendOp(new TuiEngineOp.Shutdown());
        cancel();
        if (engineThread != null) {
            try {
                engineThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
