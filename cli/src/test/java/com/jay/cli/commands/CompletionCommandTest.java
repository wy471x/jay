package com.jay.cli.commands;

import org.junit.jupiter.api.*;

import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.*;

class CompletionCommandTest {

    @Test
    void bashReturnsZero() {
        int code = new CommandLine(new CompletionCommand()).execute("bash");
        assertEquals(0, code);
    }

    @Test
    void zshReturnsZero() {
        int code = new CommandLine(new CompletionCommand()).execute("zsh");
        assertEquals(0, code);
    }

    @Test
    void fishReturnsZero() {
        int code = new CommandLine(new CompletionCommand()).execute("fish");
        assertEquals(0, code);
    }

    @Test
    void unknownShellReturnsNonZero() {
        int code = new CommandLine(new CompletionCommand()).execute("invalid");
        assertEquals(1, code);
    }

    @Test
    void missingShellFails() {
        int code = new CommandLine(new CompletionCommand()).execute();
        assertNotEquals(0, code);
    }

    @Test
    void outputFlagParses() {
        CompletionCommand cmd = new CompletionCommand();
        new CommandLine(cmd).parseArgs("-o", "/tmp/out.sh", "bash");
        assertNotNull(cmd.output);
    }
}
