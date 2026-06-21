package com.jay.cli.commands;

import org.junit.jupiter.api.*;

import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.*;

class UpdateAndMetricsCommandTest {

    @Nested
    class Update {

        @Test
        void parsesBetaFlag() {
            UpdateCommand cmd = new UpdateCommand();
            new CommandLine(cmd).parseArgs("--beta");
            assertTrue(cmd.beta);
        }

        @Test
        void parsesCheckFlag() {
            UpdateCommand cmd = new UpdateCommand();
            new CommandLine(cmd).parseArgs("--check");
            assertTrue(cmd.check);
        }

        @Test
        void parsesProxyFlag() {
            UpdateCommand cmd = new UpdateCommand();
            new CommandLine(cmd).parseArgs("--proxy", "http://proxy:3128");
            assertEquals("http://proxy:3128", cmd.proxy);
        }

        @Test
        void defaultsAreFalseAndNull() {
            UpdateCommand cmd = new UpdateCommand();
            new CommandLine(cmd).parseArgs();
            assertFalse(cmd.beta);
            assertFalse(cmd.check);
            assertNull(cmd.proxy);
        }
    }

    @Nested
    class Metrics {

        @Test
        void defaultRunReturnsZero() {
            int code = new CommandLine(new MetricsCommand()).execute();
            assertEquals(0, code);
        }

        @Test
        void jsonFlagReturnsZero() {
            int code = new CommandLine(new MetricsCommand()).execute("--json");
            assertEquals(0, code);
        }

        @Test
        void sinceFlagReturnsZero() {
            int code = new CommandLine(new MetricsCommand()).execute("--since", "7d");
            assertEquals(0, code);
        }

        @Test
        void jsonAndSinceReturnsZero() {
            int code = new CommandLine(new MetricsCommand()).execute("--json", "--since", "1h");
            assertEquals(0, code);
        }
    }
}
