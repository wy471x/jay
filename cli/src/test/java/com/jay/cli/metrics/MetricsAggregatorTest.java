package com.jay.cli.metrics;

import org.junit.jupiter.api.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class MetricsAggregatorTest {

    // ── parseSince ───────────────────────────────────────────────────

    @Nested
    class ParseSince {

        @Test
        void nullReturnsEpoch() {
            assertEquals(Instant.EPOCH, MetricsAggregator.parseSince(null));
        }

        @Test
        void emptyReturnsEpoch() {
            assertEquals(Instant.EPOCH, MetricsAggregator.parseSince(""));
            assertEquals(Instant.EPOCH, MetricsAggregator.parseSince("   "));
        }

        @Test
        void parsesDays() {
            Instant cutoff = MetricsAggregator.parseSince("7d");
            long diff = Instant.now().getEpochSecond() - cutoff.getEpochSecond();
            assertTrue(diff >= 7 * 86400 - 5, "should be ~7 days ago");
            assertTrue(diff <= 7 * 86400 + 5, "should be ~7 days ago");
        }

        @Test
        void parsesHours() {
            Instant cutoff = MetricsAggregator.parseSince("24h");
            long diff = Instant.now().getEpochSecond() - cutoff.getEpochSecond();
            assertTrue(diff >= 24 * 3600 - 5);
            assertTrue(diff <= 24 * 3600 + 5);
        }

        @Test
        void parsesMinutes() {
            Instant cutoff = MetricsAggregator.parseSince("30m");
            long diff = Instant.now().getEpochSecond() - cutoff.getEpochSecond();
            assertTrue(diff >= 30 * 60 - 5);
            assertTrue(diff <= 30 * 60 + 5);
        }

        @Test
        void parsesSeconds() {
            Instant cutoff = MetricsAggregator.parseSince("90s");
            long diff = Instant.now().getEpochSecond() - cutoff.getEpochSecond();
            assertTrue(diff >= 90 - 5);
            assertTrue(diff <= 90 + 5);
        }

        @Test
        void parsesCompoundDuration() {
            Instant cutoff = MetricsAggregator.parseSince("2h30m");
            Instant expected = Instant.now().minus(2, ChronoUnit.HOURS).minus(30, ChronoUnit.MINUTES);
            long diff = Math.abs(cutoff.getEpochSecond() - expected.getEpochSecond());
            assertTrue(diff < 10, "should be ~2h30m ago, got diff=" + diff);
        }

        @Test
        void parsesNowDashX() {
            Instant cutoff = MetricsAggregator.parseSince("now-1h");
            long diff = Instant.now().getEpochSecond() - cutoff.getEpochSecond();
            assertTrue(diff >= 3600 - 5);
            assertTrue(diff <= 3600 + 5);
        }

        @Test
        void handlesCaseInsensitively() {
            Instant c1 = MetricsAggregator.parseSince("7D");
            Instant c2 = MetricsAggregator.parseSince("7d");
            assertEquals(c1.getEpochSecond(), c2.getEpochSecond(), 5);
        }
    }

    // ── Rollup ──────────────────────────────────────────────────────

    @Nested
    class Rollup {

        @Test
        void defaultRollupHasAllZeros() {
            MetricsAggregator.Rollup r = new MetricsAggregator.Rollup();
            assertEquals(0, r.auditEvents);
            assertEquals(0, r.approvals);
            assertEquals(0, r.credentials);
            assertEquals(0, r.sessions);
            assertEquals(0, r.toolCalls);
            assertEquals(0, r.compactions);
            assertEquals(0, r.runtimeEvents);
            assertEquals(0, r.agentTurns);
        }

        @Test
        void toMapContainsAllKeys() {
            MetricsAggregator.Rollup r = new MetricsAggregator.Rollup();
            var map = r.toMap();
            assertTrue(map.containsKey("audit_events"));
            assertTrue(map.containsKey("approvals"));
            assertTrue(map.containsKey("credentials"));
            assertTrue(map.containsKey("sessions"));
            assertTrue(map.containsKey("tool_calls"));
            assertTrue(map.containsKey("compactions"));
            assertTrue(map.containsKey("runtime_events"));
            assertTrue(map.containsKey("agent_turns"));
        }

        @Test
        void toStringContainsLabels() {
            MetricsAggregator.Rollup r = new MetricsAggregator.Rollup();
            r.toolCalls = 42;
            r.sessions = 7;
            String s = r.toString();
            assertTrue(s.contains("42"));
            assertTrue(s.contains("7"));
            assertTrue(s.contains("Tool calls"));
            assertTrue(s.contains("Sessions"));
        }
    }

    // ── aggregate with empty data ────────────────────────────────────

    @Test
    void aggregateReturnsEmptyRollupWhenNoData() throws Exception {
        MetricsAggregator agg = new MetricsAggregator();
        var rollup = agg.aggregate(Instant.EPOCH);
        assertNotNull(rollup);
        assertEquals(0, rollup.auditEvents);
        assertEquals(0, rollup.sessions);
    }
}
