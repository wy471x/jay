package com.jay.cli.commands;

import org.junit.jupiter.api.*;

import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.*;

class AppServerCommandTest {

    @Test
    void defaultModeParses() {
        AppServerCommand cmd = new AppServerCommand();
        new CommandLine(cmd).parseArgs();
        assertEquals("127.0.0.1", cmd.host);
        assertEquals(7878, cmd.port);
        assertEquals(4, cmd.workers);
    }

    @Test
    void httpFlagParses() {
        AppServerCommand cmd = new AppServerCommand();
        new CommandLine(cmd).parseArgs("--http");
        assertTrue(cmd.http);
    }

    @Test
    void mobileFlagParses() {
        AppServerCommand cmd = new AppServerCommand();
        new CommandLine(cmd).parseArgs("--mobile");
        assertTrue(cmd.mobile);
    }

    @Test
    void stdioFlagParses() {
        AppServerCommand cmd = new AppServerCommand();
        new CommandLine(cmd).parseArgs("--stdio");
        assertTrue(cmd.stdio);
    }

    @Test
    void hostAndPortParse() {
        AppServerCommand cmd = new AppServerCommand();
        new CommandLine(cmd).parseArgs("--host", "0.0.0.0", "--port", "9090");
        assertEquals("0.0.0.0", cmd.host);
        assertEquals(9090, cmd.port);
    }

    @Test
    void workersParse() {
        AppServerCommand cmd = new AppServerCommand();
        new CommandLine(cmd).parseArgs("--workers", "8");
        assertEquals(8, cmd.workers);
    }

    @Test
    void authTokenParse() {
        AppServerCommand cmd = new AppServerCommand();
        new CommandLine(cmd).parseArgs("--auth-token", "secret");
        assertEquals("secret", cmd.authToken);
    }

    @Test
    void insecureNoAuthParses() {
        AppServerCommand cmd = new AppServerCommand();
        new CommandLine(cmd).parseArgs("--insecure-no-auth");
        assertTrue(cmd.insecureNoAuth);
    }

    @Test
    void corsOriginParses() {
        AppServerCommand cmd = new AppServerCommand();
        new CommandLine(cmd).parseArgs("--cors-origin", "http://example.com");
        assertNotNull(cmd.corsOrigins);
        assertEquals(1, cmd.corsOrigins.size());
    }
}
