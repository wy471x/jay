package com.jay.cli.commands;

import org.junit.jupiter.api.*;

import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.*;

class TuiDelegatedCommandsTest {

    @Test void runCommandReturnsZero() { assertEquals(0, new CommandLine(new RunCommand()).execute()); }
    @Test void execCommandReturnsZero() { assertEquals(0, new CommandLine(new ExecCommand()).execute("test")); }
    @Test void serveCommandReturnsZero() { assertEquals(0, new CommandLine(new ServeCommand()).execute()); }
    @Test void reviewCommandReturnsZero() { assertEquals(0, new CommandLine(new ReviewCommand()).execute()); }
    @Test void initCommandReturnsZero() { assertEquals(0, new CommandLine(new InitCommand()).execute()); }
    @Test void setupCommandReturnsZero() { assertEquals(0, new CommandLine(new SetupCommand()).execute()); }
    @Test void sessionsCommandReturnsZero() { assertEquals(0, new CommandLine(new SessionsCommand()).execute()); }
    @Test void resumeCommandReturnsZero() { assertEquals(0, new CommandLine(new ResumeCommand()).execute()); }
    @Test void forkCommandReturnsZero() { assertEquals(0, new CommandLine(new ForkCommand()).execute()); }
    @Test void fleetCommandReturnsZero() { assertEquals(0, new CommandLine(new FleetCommand()).execute()); }
    @Test void swebenchCommandReturnsZero() { assertEquals(0, new CommandLine(new SwebenchCommand()).execute()); }
    @Test void evalCommandReturnsZero() { assertEquals(0, new CommandLine(new EvalCommand()).execute()); }
    @Test void applyCommandReturnsZero() { assertEquals(0, new CommandLine(new ApplyCommand()).execute()); }
    @Test void speechCommandReturnsZero() { assertEquals(0, new CommandLine(new SpeechCommand()).execute()); }
    @Test void modelsCommandReturnsZero() { assertEquals(0, new CommandLine(new ModelsCommand()).execute()); }
    @Test void featuresCommandReturnsZero() { assertEquals(0, new CommandLine(new FeaturesCommand()).execute()); }
    @Test void mcpCommandReturnsZero() { assertEquals(0, new CommandLine(new McpCommand()).execute()); }
    @Test void remoteSetupCommandReturnsZero() { assertEquals(0, new CommandLine(new RemoteSetupCommand()).execute()); }

    @Test
    void execCommandParsesJsonFlag() {
        ExecCommand cmd = new ExecCommand();
        new CommandLine(cmd).parseArgs("--json", "test");
        assertTrue(cmd.json);
    }

    @Test
    void execCommandParsesAutoFlag() {
        ExecCommand cmd = new ExecCommand();
        new CommandLine(cmd).parseArgs("--auto", "test");
        assertTrue(cmd.autoMode);
    }

    @Test
    void remoteSetupParsesCloudAndBridge() {
        RemoteSetupCommand cmd = new RemoteSetupCommand();
        new CommandLine(cmd).parseArgs("--cloud", "lighthouse", "--bridge", "feishu");
        assertEquals("lighthouse", cmd.cloud);
        assertEquals("feishu", cmd.bridge);
    }
}
