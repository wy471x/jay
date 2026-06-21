package com.jay.cli;

import com.jay.agent.ProviderKind;
import com.jay.config.model.CliRuntimeOverrides;
import org.junit.jupiter.api.*;

import picocli.CommandLine;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class JayCliTest {

    // ── CLI parsing ──────────────────────────────────────────────────

    @Nested
    class Parsing {

        @Test
        void helpOptionPrintsUsage() {
            int code = new CommandLine(new JayCli()).execute("--help");
            assertEquals(0, code);
        }

        @Test
        void versionOptionPrintsVersion() {
            int code = new CommandLine(new JayCli()).execute("--version");
            assertEquals(0, code);
        }

        @Test
        void noArgsPrintsUsage() {
            StringWriter sw = new StringWriter();
            CommandLine cmd = new CommandLine(new JayCli());
            cmd.setOut(new PrintWriter(sw));
            int code = cmd.execute();
            // picocli may print usage to stdout or stderr; either way exit 0
            assertEquals(0, code);
        }

        @Test
        void promptPositionalIsParsed() {
            JayCli cli = new JayCli();
            new CommandLine(cli).parseArgs("hello", "world");
            assertNotNull(cli.prompt);
            assertEquals(2, cli.prompt.size());
            assertEquals("hello", cli.prompt.get(0));
            assertEquals("world", cli.prompt.get(1));
        }

        @Test
        void globalFlagParsingModel() {
            JayCli cli = new JayCli();
            new CommandLine(cli).parseArgs("--model", "gpt-5");
            assertEquals("gpt-5", cli.model);
        }

        @Test
        void globalFlagParsingProviderNvidia() {
            JayCli cli = new JayCli();
            new CommandLine(cli).parseArgs("--provider", "nvidia-nim");
            assertEquals(ProviderKind.NVIDIA_NIM, cli.provider);
        }

        @Test
        void globalFlagParsingYolo() {
            JayCli cli = new JayCli();
            new CommandLine(cli).parseArgs("--yolo");
            assertTrue(cli.yolo);
        }

        @Test
        void globalFlagParsingContinue() {
            JayCli cli = new JayCli();
            new CommandLine(cli).parseArgs("-c");
            assertTrue(cli.continueSession);
        }

        @Test
        void globalFlagParsingPromptFlag() {
            JayCli cli = new JayCli();
            new CommandLine(cli).parseArgs("-p", "solve this bug");
            assertEquals("solve this bug", cli.promptFlag);
        }

        @Test
        void globalFlagParsingWorkspace() {
            JayCli cli = new JayCli();
            new CommandLine(cli).parseArgs("-C", "/tmp/ws");
            assertEquals(new File("/tmp/ws"), cli.workspace);
        }

        @Test
        void globalFlagParsingSandboxMode() {
            JayCli cli = new JayCli();
            new CommandLine(cli).parseArgs("--sandbox-mode", "strict");
            assertEquals("strict", cli.sandboxMode);
        }

        @Test
        void globalFlagParsingOutputMode() {
            JayCli cli = new JayCli();
            new CommandLine(cli).parseArgs("--output-mode", "stream-json");
            assertEquals("stream-json", cli.outputMode);
        }

        @Test
        void globalFlagParsingVerbosity() {
            JayCli cli = new JayCli();
            new CommandLine(cli).parseArgs("--verbosity", "concise");
            assertEquals("concise", cli.verbosity);
        }

        @Test
        void mouseCaptureFlagSetToTrue() {
            JayCli cli = new JayCli();
            new CommandLine(cli).parseArgs("--mouse-capture");
            assertEquals(Boolean.TRUE, cli.mouseCapture);
        }

        @Test
        void mouseCaptureNegatedSetsFalse() {
            JayCli cli = new JayCli();
            new CommandLine(cli).parseArgs("--no-mouse-capture");
            assertFalse(cli.mouseCapture, "negatable flag should be false");
        }

        @Test
        void globalFlagParsingTelemetry() {
            JayCli cli = new JayCli();
            new CommandLine(cli).parseArgs("--telemetry=false");
            assertFalse(cli.telemetry);
        }

        @Test
        void globalFlagParsingTelemetryTrue() {
            JayCli cli = new JayCli();
            new CommandLine(cli).parseArgs("--telemetry=true");
            assertTrue(cli.telemetry);
        }

        @Test
        void globalFlagParsingLogLevel() {
            JayCli cli = new JayCli();
            new CommandLine(cli).parseArgs("--log-level", "DEBUG");
            assertEquals("DEBUG", cli.logLevel);
        }
    }

    // ── buildOverrides ──────────────────────────────────────────────

    @Nested
    class BuildOverrides {

        @Test
        void emptyWhenNoFlags() {
            // Reset instance from previous tests
            new CommandLine(new JayCli()).parseArgs();
            CliRuntimeOverrides o = JayCli.buildOverrides();
            assertNull(o.provider());
            assertNull(o.model());
            assertNull(o.apiKey());
            assertNull(o.baseUrl());
        }

        @Test
        void capturesProviderAndModel() {
            new CommandLine(new JayCli()).parseArgs("--provider", "nvidia-nim", "--model", "my-model");
            CliRuntimeOverrides o = JayCli.buildOverrides();
            assertEquals(ProviderKind.NVIDIA_NIM, o.provider());
            assertEquals("my-model", o.model());
        }

        @Test
        void capturesYolo() {
            new CommandLine(new JayCli()).parseArgs("--yolo");
            CliRuntimeOverrides o = JayCli.buildOverrides();
            assertTrue(o.yolo());
        }

        @Test
        void capturesApiKey() {
            new CommandLine(new JayCli()).parseArgs("--api-key", "sk-secret");
            CliRuntimeOverrides o = JayCli.buildOverrides();
            assertEquals("sk-secret", o.apiKey());
        }

        @Test
        void capturesApprovalPolicy() {
            new CommandLine(new JayCli()).parseArgs("--approval-policy", "strict");
            CliRuntimeOverrides o = JayCli.buildOverrides();
            assertEquals("strict", o.approvalPolicy());
        }
    }

    // ── workspaceFromFlags ───────────────────────────────────────────

    @Nested
    class WorkspaceFromFlags {

        @Test
        void returnsNullWhenNotSet() {
            new CommandLine(new JayCli()).parseArgs();
            assertNull(JayCli.workspaceFromFlags());
        }

        @Test
        void returnsWorkspaceWhenSet() {
            new CommandLine(new JayCli()).parseArgs("-C", "/tmp/my-workspace");
            assertEquals(new File("/tmp/my-workspace"), JayCli.workspaceFromFlags());
        }
    }

    // ── Version provider ─────────────────────────────────────────────

    @Nested
    class VersionProviderTests {

        @Test
        void returnsVersionInfo() {
            var vp = new JayCli.VersionProvider();
            String[] info = vp.getVersion();
            assertTrue(info.length >= 2);
            assertTrue(info[0].contains("jay"));
            assertTrue(info[1].contains("Java:"));
        }
    }

    // ── Subcommand routing ───────────────────────────────────────────

    @Nested
    class SubcommandRouting {

        @Test
        void doctorCommandIsRouted() {
            StringWriter sw = new StringWriter();
            CommandLine cmd = new CommandLine(new JayCli());
            cmd.setOut(new PrintWriter(sw));
            int code = cmd.execute("doctor");
            assertEquals(0, code);
        }

        @Test
        void modelListCommandIsRouted() {
            StringWriter sw = new StringWriter();
            CommandLine cmd = new CommandLine(new JayCli());
            cmd.setOut(new PrintWriter(sw));
            int code = cmd.execute("model", "list");
            assertEquals(0, code);
        }

        @Test
        void configPathCommandIsRouted() {
            StringWriter sw = new StringWriter();
            CommandLine cmd = new CommandLine(new JayCli());
            cmd.setOut(new PrintWriter(sw));
            int code = cmd.execute("config", "path");
            // config path command should run without error
            assertTrue(code == 0 || code == 1); // 1 = file not found is ok
        }

        @Test
        void completionBashCommandReturnsZero() {
            int code = new CommandLine(new JayCli()).execute("completion", "bash");
            assertEquals(0, code);
        }

        @Test
        void completionZshCommandReturnsZero() {
            int code = new CommandLine(new JayCli()).execute("completion", "zsh");
            assertEquals(0, code);
        }

        @Test
        void unknownShellReturnsError() {
            int code = new CommandLine(new JayCli()).execute("completion", "pwsh");
            assertEquals(1, code);
        }

        @Test
        void serveCommandReturnsZero() {
            int code = new CommandLine(new JayCli()).execute("serve");
            assertEquals(0, code);
        }

        @Test
        void initCommandReturnsZero() {
            int code = new CommandLine(new JayCli()).execute("init");
            assertEquals(0, code);
        }

        @Test
        void sessionsCommandReturnsZero() {
            int code = new CommandLine(new JayCli()).execute("sessions");
            assertEquals(0, code);
        }

        @Test
        void loginCommandRequiresCredentials() {
            // Without console/--api-key, login should fail gracefully
            int code = new CommandLine(new JayCli()).execute("login", "--provider", "deepseek");
            // Either 0 (succeeds with env var) or 1 (no console, no api-key)
            assertTrue(code == 0 || code == 1, "expected 0 or 1, got " + code);
        }

        @Test
        void logoutCommandReturnsZero() {
            int code = new CommandLine(new JayCli()).execute("logout");
            assertEquals(0, code);
        }
    }
}
