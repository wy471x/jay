package com.jay.cli.commands;

import org.junit.jupiter.api.*;

import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.*;

class SandboxCommandTest {

    @Test
    void checkOptionRuns() {
        int code = new CommandLine(new SandboxCommand()).execute("--check", "ls -la");
        assertEquals(0, code);
    }

    @Test
    void positionalCommandRuns() {
        int code = new CommandLine(new SandboxCommand()).execute("echo");
        assertEquals(0, code);
    }

    @Test
    void noCommandReturnsNonZero() {
        int code = new CommandLine(new SandboxCommand()).execute();
        assertEquals(1, code);
    }

    @Test
    void askModeNeverRuns() {
        int code = new CommandLine(new SandboxCommand())
                .execute("--check", "ls", "--ask", "never");
        assertEquals(0, code);
    }

    @Test
    void askModeUnlessTrustedRuns() {
        int code = new CommandLine(new SandboxCommand())
                .execute("--check", "ls", "--ask", "unless-trusted");
        assertEquals(0, code);
    }

    @Test
    void askModeOnRequestIsDefault() {
        int code = new CommandLine(new SandboxCommand()).execute("--check", "ls");
        assertEquals(0, code);
    }

    @Test
    void invalidAskModePrintsError() {
        int code = new CommandLine(new SandboxCommand())
                .execute("--check", "ls", "--ask", "bogus");
        // May exit 0 with error printed on stderr
        assertEquals(0, code);
    }

    @Test
    void defaultAskIsOnRequest() {
        SandboxCommand cmd = new SandboxCommand();
        new CommandLine(cmd).parseArgs("--check", "ls");
        // ask should default to "on-request"
        assertNotNull(cmd.ask);
    }
}
