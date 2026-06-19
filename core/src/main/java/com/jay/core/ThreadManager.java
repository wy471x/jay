package com.jay.core;

import java.util.concurrent.Executors;

/**
 * Manages virtual threads for the agent runtime.
 * Java 21 Virtual Threads (JEP 444) replace tokio's async/await —
 * each conversation/tool execution runs on its own virtual thread,
 * eliminating callback complexity.
 */
public class ThreadManager implements AutoCloseable {

    private final java.util.concurrent.ExecutorService executor;

    public ThreadManager() {
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    public Thread startVirtualThread(String name, Runnable task) {
        return Thread.ofVirtual()
                .name(name)
                .start(task);
    }

    public void execute(Runnable task) {
        executor.execute(task);
    }

    @Override
    public void close() {
        executor.close();
    }
}
