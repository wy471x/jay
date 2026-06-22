package com.jay.cli.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Aggregates usage metrics from audit logs, session files, and runtime event streams.
 * Equivalent to Rust's metrics.rs (1,026 lines).
 *
 * <p>Data sources:
 * <ul>
 *   <li>{@code ~/.deepseek/audit.log} — JSONL audit events</li>
 *   <li>{@code ~/.deepseek/sessions/*.json} — saved session files</li>
 *   <li>{@code ~/.deepseek/tasks/runtime/events/*.jsonl} — runtime event streams</li>
 * </ul>
 */
public class MetricsAggregator {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public MetricsAggregator() { }

    /** Produce a usage rollup from all data sources. */
    public Rollup aggregate(Instant since) throws IOException {
        Path base = deepseekHome();
        Rollup rollup = new Rollup();

        readAuditLog(base.resolve("audit.log"), since, rollup);
        readSessionFiles(base.resolve("sessions"), since, rollup);
        readRuntimeEvents(base.resolve("tasks").resolve("runtime").resolve("events"), since, rollup);

        return rollup;
    }

    /** Parse a human-readable duration string like "7d", "24h", "30m", "90s", "2h30m". */
    public static Instant parseSince(String since) {
        if (since == null || since.isBlank()) return Instant.EPOCH;
        String s = since.trim().toLowerCase();
        // Handle "now-X" format
        if (s.startsWith("now-")) s = s.substring(4);
        Duration d = Duration.ZERO;
        StringBuilder num = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isDigit(c)) {
                num.append(c);
            } else {
                int val = num.isEmpty() ? 1 : Integer.parseInt(num.toString());
                num.setLength(0);
                d = switch (c) {
                    case 'd' -> d.plusDays(val);
                    case 'h' -> d.plusHours(val);
                    case 'm' -> d.plusMinutes(val);
                    case 's' -> d.plusSeconds(val);
                    default -> d;
                };
            }
        }
        return Instant.now().minus(d);
    }

    // ── Data source readers ──────────────────────────────────────────

    private void readAuditLog(Path path, Instant since, Rollup rollup) {
        if (!Files.exists(path)) return;
        try (var lines = Files.lines(path)) {
            lines.forEach(line -> {
                try {
                    var node = MAPPER.readTree(line);
                    Instant ts = parseTimestamp(node);
                    if (ts != null && ts.isBefore(since)) return;
                    String event = node.has("event") ? node.get("event").asText() : "";
                    if (event.contains("credential") || event.contains("auth")) rollup.credentials++;
                    else if (event.contains("approval")) rollup.approvals++;
                    else rollup.auditEvents++;
                } catch (Exception ignored) { }
            });
        } catch (IOException ignored) { }
    }

    private void readSessionFiles(Path dir, Instant since, Rollup rollup) {
        if (!Files.isDirectory(dir)) return;
        try (var entries = Files.list(dir)) {
            entries.filter(p -> p.toString().endsWith(".json")).forEach(path -> {
                try {
                    String content = Files.readString(path);
                    var node = MAPPER.readTree(content);
                    Instant ts = parseTimestamp(node);
                    if (ts != null && ts.isBefore(since)) return;
                    rollup.sessions++;
                    if (node.has("tools")) {
                        var tools = node.get("tools");
                        if (tools.isArray()) rollup.toolCalls += tools.size();
                    }
                    if (node.has("compactions")) {
                        rollup.compactions += node.get("compactions").asInt();
                    }
                } catch (Exception ignored) { }
            });
        } catch (IOException ignored) { }
    }

    private void readRuntimeEvents(Path dir, Instant since, Rollup rollup) {
        if (!Files.isDirectory(dir)) return;
        try (var entries = Files.list(dir)) {
            entries.filter(p -> p.toString().endsWith(".jsonl")).forEach(path -> {
                try (var lines = Files.lines(path)) {
                    lines.forEach(line -> {
                        try {
                            var node = MAPPER.readTree(line);
                            Instant ts = parseTimestamp(node);
                            if (ts != null && ts.isBefore(since)) return;
                            rollup.runtimeEvents++;
                            if (node.has("agent") || node.has("model")) rollup.agentTurns++;
                        } catch (Exception ignored) { }
                    });
                } catch (IOException ignored) { }
            });
        } catch (IOException ignored) { }
    }

    private Instant parseTimestamp(com.fasterxml.jackson.databind.JsonNode node) {
        String ts = null;
        if (node.has("timestamp")) ts = node.get("timestamp").asText(null);
        else if (node.has("ts")) ts = node.get("ts").asText(null);
        else if (node.has("created_at")) ts = node.get("created_at").asText(null);
        if (ts == null) return null;
        try {
                return Instant.parse(ts);
                } catch (Exception e) { return null;
            }
    }

    // ── Home directory ────────────────────────────────────────────────

    static Path deepseekHome() {
        String home = System.getProperty("user.home");
        return Path.of(home, ".deepseek");
    }

    // ── Rollup ────────────────────────────────────────────────────────

    public static class Rollup {
        public long auditEvents;
        public long approvals;
        public long credentials;
        public long sessions;
        public long toolCalls;
        public long compactions;
        public long runtimeEvents;
        public long agentTurns;

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("audit_events", auditEvents);
            map.put("approvals", approvals);
            map.put("credentials", credentials);
            map.put("sessions", sessions);
            map.put("tool_calls", toolCalls);
            map.put("compactions", compactions);
            map.put("runtime_events", runtimeEvents);
            map.put("agent_turns", agentTurns);
            return map;
        }

        @Override
        public String toString() {
            return String.format("""
                Usage Metrics
                  Audit events:    %d
                  Approvals:       %d
                  Credentials:     %d
                  Sessions:        %d
                  Tool calls:      %d
                  Compactions:     %d
                  Runtime events:  %d
                  Agent turns:     %d""",
                auditEvents, approvals, credentials, sessions,
                toolCalls, compactions, runtimeEvents, agentTurns);
        }
    }
}
