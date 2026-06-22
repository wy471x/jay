package com.jay.cli.commands;

import com.jay.cli.CliSpringContext;
import com.jay.state.model.ThreadEntity;
import com.jay.state.model.ThreadListFilters;
import com.jay.state.store.StateStore;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**
 * Manage thread/session metadata.
 *
 * <p>Subcommands: list, read, resume, fork, archive, unarchive, set-name, clear-name
 */
@Command(name = "thread", description = "Manage thread/session metadata",
    subcommands = {
        ThreadCommand.ListCmd.class, ThreadCommand.Read.class,
        ThreadCommand.Resume.class, ThreadCommand.Fork.class,
        ThreadCommand.Archive.class, ThreadCommand.Unarchive.class,
        ThreadCommand.SetName.class, ThreadCommand.ClearName.class
    })
public class ThreadCommand implements Callable<Integer> {
    private static final Logger LOGGER = Logger.getLogger(ThreadCommand.class.getName());

    @Override
    public Integer call() {
        LOGGER.info("Usage: jay thread <list|read|resume|fork|archive|unarchive|set-name|clear-name>");
        return 0;
    }

    private static StateStore stateStore() {
        var store = CliSpringContext.getBeanOrNull(StateStore.class);
        if (store == null) {
            LOGGER.severe("State store not available. Ensure the Spring context is initialized.");
        }
        return store;
    }

    // ── list ───────────────────────────────────────────────────────────

    @Command(name = "list", description = "List threads/sessions")
    static class ListCmd implements Callable<Integer> {
        @Option(names = {"--all"}, description = "Include archived threads")
        boolean all;

        @Option(names = {"--limit"}, description = "Max threads to show")
        int limit = 20;

        @Override
        public Integer call() {
            StateStore store = stateStore();
            if (store == null) return 1;

            var filters = new ThreadListFilters(all, limit);
            List<ThreadEntity> threads = store.listThreads(filters);
            if (threads.isEmpty()) {
                LOGGER.info("No threads found.");
                return 0;
            }
            System.out.printf("%-40s %-10s %-20s %-30s%n", "ID", "STATUS", "UPDATED", "TITLE");
            LOGGER.info("-".repeat(100));
            for (var t : threads) {
                System.out.printf("%-40s %-10s %-20s %-30s%n",
                        t.id(), t.status(), t.updatedAt(), truncate(t.title(), 28));
            }
            LOGGER.info("-".repeat(100));
            LOGGER.info(threads.size() + " thread(s)");
            return 0;
        }
    }

    // ── read ──────────────────────────────────────────────────────────

    @Command(name = "read", description = "Read thread details by ID")
    static class Read implements Callable<Integer> {
        @Parameters(index = "0", description = "Thread ID")
        String threadId;

        @Override
        public Integer call() {
            StateStore store = stateStore();
            if (store == null) return 1;

            var thread = store.readThread(threadId);
            if (thread.isEmpty()) {
                LOGGER.info("Thread not found: " + threadId);
                return 1;
            }
            var t = thread.get();
            LOGGER.info("Thread: " + t.id());
            LOGGER.info("  Status:     " + t.status());
            LOGGER.info("  Title:      " + (t.title() != null ? t.title() : "(untitled)"));
            LOGGER.info("  Created:    " + t.createdAt());
            LOGGER.info("  Updated:    " + t.updatedAt());
            LOGGER.info("  Archived:   " + (t.archivedAt() > 0 ? Instant.ofEpochMilli(t.archivedAt()).toString() : "no"));
            LOGGER.info("  Leaf ID:    " + (t.currentLeafId() > 0 ? t.currentLeafId() : "none"));
            return 0;
        }
    }

    // ── resume ────────────────────────────────────────────────────────

    @Command(name = "resume", description = "Resume a thread by ID")
    static class Resume implements Callable<Integer> {
        @Parameters(index = "0", description = "Thread ID to resume")
        String threadId;

        @Override
        public Integer call() {
            LOGGER.info("Resume thread: " + threadId);
            LOGGER.info("(use 'jay exec --resume " + threadId + " <prompt>' to continue)");
            return 0;
        }
    }

    // ── fork ──────────────────────────────────────────────────────────

    @Command(name = "fork", description = "Fork a thread by ID")
    static class Fork implements Callable<Integer> {
        @Parameters(index = "0", description = "Thread ID to fork")
        String threadId;

        @Override
        public Integer call() {
            LOGGER.info("Fork thread: " + threadId);
            LOGGER.info("Fork not yet implemented — use TUI or app-server.");
            return 0;
        }
    }

    // ── archive ───────────────────────────────────────────────────────

    @Command(name = "archive", description = "Archive a thread by ID")
    static class Archive implements Callable<Integer> {
        @Parameters(index = "0", description = "Thread ID to archive")
        String threadId;

        @Override
        public Integer call() {
            StateStore store = stateStore();
            if (store == null) return 1;
            store.archiveThread(threadId, Instant.now().toEpochMilli());
            LOGGER.info("Archived thread: " + threadId);
            return 0;
        }
    }

    // ── unarchive ─────────────────────────────────────────────────────

    @Command(name = "unarchive", description = "Unarchive a thread by ID")
    static class Unarchive implements Callable<Integer> {
        @Parameters(index = "0", description = "Thread ID to unarchive")
        String threadId;

        @Override
        public Integer call() {
            StateStore store = stateStore();
            if (store == null) return 1;
            store.unarchiveThread(threadId);
            LOGGER.info("Unarchived thread: " + threadId);
            return 0;
        }
    }

    // ── set-name ──────────────────────────────────────────────────────

    @Command(name = "set-name", description = "Set a custom name for a thread")
    static class SetName implements Callable<Integer> {
        @Parameters(index = "0", description = "Thread ID")
        String threadId;

        @Parameters(index = "1", description = "Thread name")
        String name;

        @Override
        public Integer call() {
            StateStore store = stateStore();
            if (store == null) return 1;
            store.setThreadName(threadId, name);
            LOGGER.info("Thread name set: " + threadId + " → " + name);
            return 0;
        }
    }

    // ── clear-name ────────────────────────────────────────────────────

    @Command(name = "clear-name", description = "Clear custom name for a thread")
    static class ClearName implements Callable<Integer> {
        @Parameters(index = "0", description = "Thread ID")
        String threadId;

        @Override
        public Integer call() {
            StateStore store = stateStore();
            if (store == null) return 1;
            store.clearThreadName(threadId);
            LOGGER.info("Thread name cleared: " + threadId);
            return 0;
        }
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "(unnamed)";
        return s.length() <= maxLen ? s : s.substring(0, maxLen - 1) + "…";
    }
}
