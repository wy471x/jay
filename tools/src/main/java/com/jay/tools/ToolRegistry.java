package com.jay.tools;

import com.jay.protocol.core.ToolKind;
import com.jay.protocol.core.ToolOutput;
import com.jay.protocol.core.ToolPayload;

import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.springframework.stereotype.Component;

/**
 * Central registry that maps tool names to their specs and handlers.
 * Equivalent to Rust's ToolRegistry with concurrent execution control.
 */
@Component
public class ToolRegistry {

    private final Map<String, ToolHandler> handlers = new ConcurrentHashMap<>();
    private final Map<String, ConfiguredToolSpec> specs = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock executionLock = new ReentrantReadWriteLock();
    private static final ThreadLocal<Boolean> LOCK_HELD = ThreadLocal.withInitial(() -> false);

    public record ConfiguredToolSpec(ToolSpec spec, boolean supportsParallelToolCalls) { }

    public void register(ToolSpec spec, ToolHandler handler) {
        specs.put(spec.name(), new ConfiguredToolSpec(spec, spec.supportsParallelToolCalls()));
        handlers.put(spec.name(), handler);
    }

    public List<ConfiguredToolSpec> listSpecs() {
        return List.copyOf(specs.values());
    }

    public ToolOutput dispatch(ToolCall call, boolean allowMutating) {
        var handler = handlers.get(call.name());
        if (handler == null) new FunctionCallError.ToolNotFound(call.name()).throwUnchecked();

        var configured = specs.get(call.name());
        if (configured == null) new FunctionCallError.ToolNotFound(call.name()).throwUnchecked();

        var payloadKind = toolPayloadKind(call.payload());
        if (!handler.matchesKind(payloadKind)) {
            new FunctionCallError.KindMismatch(handler.kind().name(), payloadKind.name()).throwUnchecked();
        }
        if (handler.isMutating() && !allowMutating) {
            new FunctionCallError.MutatingToolRejected(call.name()).throwUnchecked();
        }

        var callId = call.rawToolCallId() != null
                ? call.rawToolCallId() : "tool-call-" + UUID.randomUUID();
        var invocation = new ToolInvocation(callId, call.name(), call.payload(), call.source());

        var lock = configured.supportsParallelToolCalls()
                ? executionLock.readLock() : executionLock.writeLock();
        boolean reentrant = LOCK_HELD.get();
        if (!reentrant) lock.lock();
        LOCK_HELD.set(true);
        try {
            return executeWithTimeout(handler, configured.spec().timeoutMs(), invocation);
        } finally {
            LOCK_HELD.set(false);
            if (!reentrant) lock.unlock();
        }
    }

    private ToolOutput executeWithTimeout(ToolHandler handler, Long timeoutMs, ToolInvocation inv) {
        if (timeoutMs != null) {
            var name = inv.toolName();
            var future = new java.util.concurrent.FutureTask<>(() -> handler.handle(inv));
            try {
                future.run();
                return future.get(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                future.cancel(true);
                new FunctionCallError.TimedOut(name, timeoutMs).throwUnchecked();
            } catch (Exception e) {
                new FunctionCallError.ExecutionFailed(name, e.getMessage()).throwUnchecked();
            }
        }
        return handler.handle(inv);
    }

    private static ToolKind toolPayloadKind(ToolPayload payload) {
        if (payload instanceof ToolPayload.Mcp) return ToolKind.MCP;
        return ToolKind.FUNCTION;
    }
}
