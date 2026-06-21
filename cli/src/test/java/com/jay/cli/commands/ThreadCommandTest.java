package com.jay.cli.commands;

import org.junit.jupiter.api.*;

import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.*;

class ThreadCommandTest {

    @Test
    void topLevelReturnsZero() {
        int code = new CommandLine(new ThreadCommand()).execute();
        assertEquals(0, code);
    }

    @Test
    void readWithoutIdFails() {
        int code = new CommandLine(new ThreadCommand()).execute("read");
        assertNotEquals(0, code);
    }

    @Test
    void archiveWithoutIdFails() {
        int code = new CommandLine(new ThreadCommand()).execute("archive");
        assertNotEquals(0, code);
    }

    @Test
    void unarchiveWithoutIdFails() {
        int code = new CommandLine(new ThreadCommand()).execute("unarchive");
        assertNotEquals(0, code);
    }

    @Test
    void setnameWithoutTwoArgsFails() {
        int code = new CommandLine(new ThreadCommand()).execute("set-name");
        assertNotEquals(0, code);
    }

    @Test
    void clearnameWithoutIdFails() {
        int code = new CommandLine(new ThreadCommand()).execute("clear-name");
        assertNotEquals(0, code);
    }

    @Test
    void listFailsWithoutSpringContext() {
        int code = new CommandLine(new ThreadCommand()).execute("list");
        assertEquals(1, code);
    }

    @Test
    void listWithAllFlag() {
        int code = new CommandLine(new ThreadCommand()).execute("list", "--all");
        assertEquals(1, code);
    }

    @Test
    void listWithLimit() {
        int code = new CommandLine(new ThreadCommand()).execute("list", "--limit", "10");
        assertEquals(1, code);
    }

    @Test
    void forkWithoutIdFails() {
        int code = new CommandLine(new ThreadCommand()).execute("fork");
        assertNotEquals(0, code);
    }
}
