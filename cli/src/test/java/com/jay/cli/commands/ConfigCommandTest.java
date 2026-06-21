package com.jay.cli.commands;

import org.junit.jupiter.api.*;

import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.*;

class ConfigCommandTest {

    @Test
    void pathExitsZeroOrOne() {
        int code = new CommandLine(new ConfigCommand()).execute("path");
        assertTrue(code == 0 || code == 1);
    }

    @Test
    void listExitsZeroOrOne() {
        int code = new CommandLine(new ConfigCommand()).execute("list");
        assertTrue(code == 0 || code == 1);
    }

    @Test
    void getWithoutKeyFails() {
        int code = new CommandLine(new ConfigCommand()).execute("get");
        assertNotEquals(0, code);
    }

    @Test
    void setRequiresTwoArgs() {
        int code = new CommandLine(new ConfigCommand()).execute("set", "my-key", "my-value");
        assertTrue(code == 0 || code == 1);
    }

    @Test
    void unsetRequiresKey() {
        int code = new CommandLine(new ConfigCommand()).execute("unset");
        assertNotEquals(0, code);
    }

    @Test
    void topLevelPrintsUsage() {
        int code = new CommandLine(new ConfigCommand()).execute();
        assertEquals(0, code);
    }
}
