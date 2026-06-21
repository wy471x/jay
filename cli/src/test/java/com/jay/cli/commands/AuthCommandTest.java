package com.jay.cli.commands;

import org.junit.jupiter.api.*;

import picocli.CommandLine;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

class AuthCommandTest {

    @Test
    void statusParsesProvider() {
        AuthCommand.Status cmd = new AuthCommand.Status();
        new CommandLine(cmd).parseArgs("--provider", "deepseek");
        assertNotNull(cmd.provider);
    }

    @Test
    void statusWithoutProviderReturnsZero() {
        int code = new CommandLine(new AuthCommand()).execute("status");
        assertEquals(0, code);
    }

    @Test
    void statusWithProviderReturnsZero() {
        int code = new CommandLine(new AuthCommand()).execute("status", "--provider", "deepseek");
        assertEquals(0, code);
    }

    @Test
    void listReturnsZero() {
        int code = new CommandLine(new AuthCommand()).execute("list");
        assertEquals(0, code);
    }

    @Test
    void getWithoutProviderFails() {
        int code = new CommandLine(new AuthCommand()).execute("get");
        assertNotEquals(0, code);
    }

    @Test
    void clearWithoutProviderFails() {
        int code = new CommandLine(new AuthCommand()).execute("clear");
        assertNotEquals(0, code);
    }

    @Test
    void setWithoutProviderFails() {
        int code = new CommandLine(new AuthCommand()).execute("set", "--api-key", "test");
        assertNotEquals(0, code);
    }

    @Test
    void setWithProviderNeedsApiKey() {
        int code = new CommandLine(new AuthCommand()).execute("set", "--provider", "deepseek");
        assertNotEquals(0, code); // no api-key provided
    }

    @Test
    void migrateDryRunReturnsZero() {
        int code = new CommandLine(new AuthCommand()).execute("migrate", "--dry-run");
        assertEquals(0, code);
    }

    @Test
    void topLevelPrintsUsageReturnsZero() {
        int code = new CommandLine(new AuthCommand()).execute();
        assertEquals(0, code);
    }

    @Test
    void getWithProviderReturnsZero() {
        int code = new CommandLine(new AuthCommand()).execute("get", "--provider", "deepseek");
        assertEquals(0, code);
    }

    @Test
    void clearWithProviderReturnsZero() {
        int code = new CommandLine(new AuthCommand()).execute("clear", "--provider", "deepseek");
        assertEquals(0, code);
    }
}
