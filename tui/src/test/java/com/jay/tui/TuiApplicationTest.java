package com.jay.tui;

import com.jay.agent.ProviderKind;
import com.jay.tui.commands.BuiltinCommands;
import com.jay.tui.commands.CommandRegistry;
import com.jay.tui.core.*;
import com.jay.tui.core.compaction.CompactionConfig;
import com.jay.tui.core.turn.SseTurnLoop;
import com.jay.tui.input.SlashMenu;
import com.jay.tui.state.AppState;
import com.jay.tui.state.ComposerState;
import com.jay.tui.state.StatusBarState;
import org.junit.jupiter.api.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the 7-step main path in {@link TuiApplication}.
 *
 * <p>Covers: CLI arg parsing, EngineConfig construction, engine lifecycle,
 * applyEngineEvent translation for all 28 event variants, event context wiring,
 * and the full initialization chain.
 */
@DisplayName("TUI Application 7-Step Main Path")
class TuiApplicationTest {

    // ═══════════════════════════════════════════════════════════════════
    // 1. CLI Arg Parsing (Step 4 — CLI → EngineConfig)
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("CLI Arg Parsing")
    class CliArgParsing {

        @Test
        @DisplayName("parseArgs handles null and empty arrays")
        void nullAndEmptyArgs() {
            Map<String, String> nullResult = TuiApplication.parseArgs(null);
            assertTrue(nullResult.isEmpty());

            Map<String, String> emptyResult = TuiApplication.parseArgs(new String[0]);
            assertTrue(emptyResult.isEmpty());
        }

        @Test
        @DisplayName("parseArgs extracts model, workspace, max-steps with values")
        void keyValueArgs() {
            var args = TuiApplication.parseArgs(new String[]{
                    "--model", "claude-opus-4", "--workspace", "/tmp/project",
                    "--max-steps", "50"
            });

            assertEquals("claude-opus-4", args.get("model"));
            assertEquals("/tmp/project", args.get("workspace"));
            assertEquals("50", args.get("maxSteps"));
        }

        @Test
        @DisplayName("parseArgs handles boolean flags")
        void booleanFlags() {
            var args = TuiApplication.parseArgs(new String[]{
                    "--allow-shell", "--show-thinking", "--compaction",
                    "--trust", "--no-memory"
            });

            assertEquals("true", args.get("allowShell"));
            assertEquals("true", args.get("showThinking"));
            assertEquals("true", args.get("compaction"));
            assertEquals("true", args.get("trustMode"));
            assertEquals("false", args.get("memoryEnabled"));
        }

        @Test
        @DisplayName("parseArgs handles negation flags")
        void negationFlags() {
            var args = TuiApplication.parseArgs(new String[]{
                    "--no-shell", "--no-thinking"
            });

            assertEquals("false", args.get("allowShell"));
            assertEquals("false", args.get("showThinking"));
        }

        @Test
        @DisplayName("parseArgs ignores unknown flags")
        void unknownFlagsIgnored() {
            var args = TuiApplication.parseArgs(new String[]{
                    "--unknown-flag", "--model", "gpt-5", "--verbose"
            });

            assertEquals("gpt-5", args.get("model"));
            assertNull(args.get("unknown-flag"));
            assertNull(args.get("verbose"));
        }

        @Test
        @DisplayName("parseArgs handles positional arguments")
        void positionalArgs() {
            var args = TuiApplication.parseArgs(new String[]{
                    "hello.txt", "--model", "m1"
            });

            assertEquals("hello.txt", args.get("positional"));
            assertEquals("m1", args.get("model"));
        }

        @Test
        @DisplayName("parseArgs handles missing value for flag")
        void missingValue() {
            var args = TuiApplication.parseArgs(new String[]{"--model"});
            assertEquals("", args.get("model"));

            var args2 = TuiApplication.parseArgs(new String[]{"--model", "--workspace"});
            assertEquals("", args2.get("model"));
            assertEquals("", args2.get("workspace"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 2. EngineConfig Construction (Step 4)
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("EngineConfig Construction")
    class EngineConfigConstruction {

        @Test
        @DisplayName("buildEngineConfig with empty args returns defaults")
        void emptyArgsDefaults() {
            var config = TuiApplication.buildEngineConfig(Map.of());
            assertEquals("deepseek-v4-pro", config.model());
            assertEquals(Path.of("."), config.workspace());
            assertTrue(config.allowShell());
            assertTrue(config.showThinking());
            assertTrue(config.memoryEnabled());
            assertEquals(100, config.maxSteps());
            // v0.8.64: compaction is ON by default with 800K threshold
            assertTrue(config.compaction().enabled());
            assertEquals(CompactionConfig.DEFAULT_TOKEN_THRESHOLD,
                    config.compaction().tokenThreshold());
        }

        @Test
        @DisplayName("buildEngineConfig overrides model and workspace")
        void overrideModelAndWorkspace() {
            var config = TuiApplication.buildEngineConfig(Map.of(
                    "model", "claude-4", "workspace", "/tmp/ws"
            ));
            assertEquals("claude-4", config.model());
            assertEquals(Path.of("/tmp/ws"), config.workspace());
        }

        @Test
        @DisplayName("buildEngineConfig overrides allowShell and showThinking")
        void overrideBooleanFlags() {
            var config = TuiApplication.buildEngineConfig(Map.of(
                    "allowShell", "false", "showThinking", "false"
            ));
            assertFalse(config.allowShell());
            assertFalse(config.showThinking());
        }

        @Test
        @DisplayName("buildEngineConfig overrides maxSteps")
        void overrideMaxSteps() {
            var config = TuiApplication.buildEngineConfig(Map.of("maxSteps", "25"));
            assertEquals(25, config.maxSteps());
        }

        @Test
        @DisplayName("buildEngineConfig handles invalid maxSteps gracefully")
        void invalidMaxSteps() {
            var config = TuiApplication.buildEngineConfig(Map.of("maxSteps", "abc"));
            assertEquals(100, config.maxSteps()); // keeps default
        }

        @Test
        @DisplayName("buildEngineConfig enables compaction")
        void enableCompaction() {
            var config = TuiApplication.buildEngineConfig(Map.of("compaction", "true"));
            assertTrue(config.compaction().enabled());
            assertEquals(4000, config.compaction().tokenThreshold());
        }

        @Test
        @DisplayName("buildEngineConfig enables trust mode")
        void enableTrustMode() {
            var config = TuiApplication.buildEngineConfig(Map.of("trustMode", "true"));
            assertTrue(config.trustMode());
        }

        @Test
        @DisplayName("buildEngineConfig disables memory")
        void disableMemory() {
            var config = TuiApplication.buildEngineConfig(
                    Map.of("memoryEnabled", "false"));
            assertFalse(config.memoryEnabled());
        }

        @Test
        @DisplayName("buildEngineConfig respects system property overrides")
        void systemPropertyOverrides() {
            System.setProperty("jay.model", "sys-model");
            System.setProperty("jay.workspace", "/tmp/sys-ws");
            try {
                var config = TuiApplication.buildEngineConfig(Map.of());
                assertEquals("sys-model", config.model());
                assertEquals(Path.of("/tmp/sys-ws"), config.workspace());
            } finally {
                System.clearProperty("jay.model");
                System.clearProperty("jay.workspace");
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 3. applyEngineEvent — All 28 Event Variants (Step 7)
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("applyEngineEvent Translation")
    class ApplyEngineEventTests {

        private AppState appState;
        private TuiView view;

        @BeforeEach
        void setUp() {
            appState = new AppState();
            var composer = new ComposerState();
            var registry = new CommandRegistry();
            var slash = new SlashMenu(registry);
            view = new TuiView(appState, composer, slash, registry);
        }

        @Test
        @DisplayName("Streaming events: MessageStarted, MessageDelta, MessageComplete")
        void streamingEvents() {
            TuiApplication.applyEngineEvent(appState, view,
                    new TuiEngineEvent.MessageStarted(0, "text"));
            TuiApplication.applyEngineEvent(appState, view,
                    new TuiEngineEvent.MessageDelta(0, "Hello"));
            TuiApplication.applyEngineEvent(appState, view,
                    new TuiEngineEvent.MessageDelta(0, " World"));
            TuiApplication.applyEngineEvent(appState, view,
                    new TuiEngineEvent.MessageComplete(0, "text"));

            var lines = view.getTranscriptLines();
            assertTrue(lines.stream().anyMatch(l -> l.contains("Hello")),
                    "Transcript should contain message delta");
        }

        @Test
        @DisplayName("Thinking events: ThinkingStarted, ThinkingDelta, ThinkingComplete")
        void thinkingEvents() {
            TuiApplication.applyEngineEvent(appState, view,
                    new TuiEngineEvent.ThinkingStarted(0));
            assertEquals("Thinking...", appState.statusBar().statusMessage());

            TuiApplication.applyEngineEvent(appState, view,
                    new TuiEngineEvent.ThinkingDelta(0, "analyzing..."));
            var lines = view.getTranscriptLines();
            assertTrue(lines.stream().anyMatch(l -> l.contains("[thinking]")));

            TuiApplication.applyEngineEvent(appState, view,
                    new TuiEngineEvent.ThinkingComplete(0));
            assertEquals("Ready", appState.statusBar().statusMessage());
        }

        @Test
        @DisplayName("Tool events: ToolCallStarted, ToolCallComplete (success)")
        void toolCallSuccess() {
            TuiApplication.applyEngineEvent(appState, view,
                    new TuiEngineEvent.ToolCallStarted("t1", "read",
                            "{\"path\":\"/tmp\"}"));
            var lines = view.getTranscriptLines();
            assertTrue(lines.stream().anyMatch(l -> l.contains("[tool:read]")));

            var result = TuiEngineEvent.ToolResult.success("t1", "read", "OK");
            TuiApplication.applyEngineEvent(appState, view,
                    new TuiEngineEvent.ToolCallComplete("t1", "read", result));
            lines = view.getTranscriptLines();
            assertTrue(lines.stream().anyMatch(l -> l.contains("OK")));
        }

        @Test
        @DisplayName("Tool events: ToolCallComplete (failure)")
        void toolCallFailure() {
            var result = TuiEngineEvent.ToolResult.failure("t1", "bash", "permission denied");
            TuiApplication.applyEngineEvent(appState, view,
                    new TuiEngineEvent.ToolCallComplete("t1", "bash", result));

            var lines = view.getTranscriptLines();
            assertTrue(lines.stream().anyMatch(
                    l -> l.contains("ERR:") && l.contains("permission denied")));
        }

        @Test
        @DisplayName("Tool events: ApprovalRequired")
        void approvalRequired() {
            TuiApplication.applyEngineEvent(appState, view,
                    new TuiEngineEvent.ApprovalRequired("a1", "bash",
                            "Run: rm -rf /", "Deleting everything"));

            var lines = view.getTranscriptLines();
            assertTrue(lines.stream().anyMatch(
                    l -> l.contains("approval needed") && l.contains("bash")));
        }

        @Test
        @DisplayName("Turn lifecycle: TurnStarted, TurnComplete, TurnAborted")
        void turnLifecycle() {
            TuiApplication.applyEngineEvent(appState, view,
                    new TuiEngineEvent.TurnStarted("turn-abc123"));
            var lines = view.getTranscriptLines();
            assertTrue(lines.stream().anyMatch(l -> l.contains("turn-abc123")));

            TuiApplication.applyEngineEvent(appState, view,
                    new TuiEngineEvent.TurnComplete("turn-abc123", 100, 50,
                            TuiEngineEvent.TurnOutcomeStatus.COMPLETED, null));
            lines = view.getTranscriptLines();
            assertTrue(lines.stream().anyMatch(l -> l.contains("Turn complete")));

            TuiApplication.applyEngineEvent(appState, view,
                    new TuiEngineEvent.TurnAborted("turn-xyz", "timeout"));
            lines = view.getTranscriptLines();
            assertTrue(lines.stream().anyMatch(
                    l -> l.contains("aborted") && l.contains("timeout")));
        }

        @Test
        @DisplayName("Compaction events")
        void compactionEvents() {
            TuiApplication.applyEngineEvent(appState, view,
                    new TuiEngineEvent.CompactionStarted("c1", true,
                            "threshold reached"));
            assertTrue(appState.statusBar().statusMessage().contains("Compacting"));

            TuiApplication.applyEngineEvent(appState, view,
                    new TuiEngineEvent.CompactionCompleted("c1", true,
                            "done", 50, 20));
            assertTrue(appState.statusBar().statusMessage().contains("50→20"));

            TuiApplication.applyEngineEvent(appState, view,
                    new TuiEngineEvent.CompactionFailed("c1", true, "no model"));
            assertTrue(appState.statusBar().statusMessage().contains("Compaction failed"));
        }

        @Test
        @DisplayName("Purge events")
        void purgeEvents() {
            TuiApplication.applyEngineEvent(appState, view,
                    new TuiEngineEvent.PurgeStarted("cleaning up"));
            assertTrue(appState.statusBar().statusMessage().contains("Purging"));

            TuiApplication.applyEngineEvent(appState, view,
                    new TuiEngineEvent.PurgeCompleted(30, 15, 10, 5, "done"));
            assertTrue(appState.statusBar().statusMessage().contains("10 removed"));
            assertTrue(appState.statusBar().statusMessage().contains("5 replaced"));

            TuiApplication.applyEngineEvent(appState, view,
                    new TuiEngineEvent.PurgeFailed("disk full"));
            assertTrue(appState.statusBar().statusMessage().contains("Purge failed"));
        }

        @Test
        @DisplayName("System events: StatusMessage, EngineError, EngineInitialized")
        void systemEvents() {
            TuiApplication.applyEngineEvent(appState, view,
                    new TuiEngineEvent.StatusMessage("Ready", "info"));
            assertEquals("Ready", appState.statusBar().statusMessage());

            TuiApplication.applyEngineEvent(appState, view,
                    new TuiEngineEvent.EngineError("Connection refused",
                            "network", true));
            assertEquals("Connection refused", appState.statusBar().statusMessage());
            assertEquals(StatusBarState.Severity.ERROR, appState.statusBar().severity());

            TuiApplication.applyEngineEvent(appState, view,
                    new TuiEngineEvent.EngineInitialized("gpt-5", "my-workspace",
                            List.of(), "thread-1"));
            assertEquals("gpt-5", appState.statusBar().modelName());
        }

        @Test
        @DisplayName("System events: SessionUpdated, PrefixCacheChange, ModelChanged")
        void systemEventVariants() {
            TuiApplication.applyEngineEvent(appState, view,
                    new TuiEngineEvent.SessionUpdated("s1", 42, 10000L));
            assertTrue(appState.statusBar().statusMessage().contains("42 msgs"));
            assertTrue(appState.statusBar().statusMessage().contains("10000 tokens"));

            TuiApplication.applyEngineEvent(appState, view,
                    new TuiEngineEvent.PrefixCacheChange("stable",
                            false, false, 95.5));
            assertTrue(appState.statusBar().statusMessage().contains("Cache"));

            TuiApplication.applyEngineEvent(appState, view,
                    new TuiEngineEvent.ModelChanged("claude-opus-4",
                            ProviderKind.ANTHROPIC));
            assertEquals("claude-opus-4", appState.statusBar().modelName());
            assertEquals(ProviderKind.ANTHROPIC, appState.statusBar().provider());
        }

        @Test
        @DisplayName("I/O events: UserInputRequired, PauseEvents, ResumeEvents")
        void ioEvents() {
            TuiApplication.applyEngineEvent(appState, view,
                    new TuiEngineEvent.UserInputRequired("ui1", "Enter name", "default"));
            assertTrue(appState.statusBar().statusMessage().contains("Input needed"));

            TuiApplication.applyEngineEvent(appState, view,
                    new TuiEngineEvent.PauseEvents());
            assertTrue(appState.statusBar().statusMessage().contains("paused"));

            TuiApplication.applyEngineEvent(appState, view,
                    new TuiEngineEvent.ResumeEvents());
            assertEquals("Ready", appState.statusBar().statusMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 4. 7-Step Main Path Initialization (Steps 1-5)
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("7-Step Main Path Initialization")
    class SevenStepInit {

        @Test
        @DisplayName("step 1: AppState initializes with defaults")
        void step1AppState() {
            var appState = new AppState();
            assertNotNull(appState);
            assertFalse(appState.initialized());
            assertFalse(appState.shuttingDown());
            assertNotNull(appState.composer());
            assertNotNull(appState.viewport());
            assertNotNull(appState.sidebar());
            assertNotNull(appState.statusBar());
            assertEquals("deepseek-v4-flash", appState.currentModel());
            assertEquals(ProviderKind.DEEPSEEK, appState.currentProvider());
        }

        @Test
        @DisplayName("step 2: ComposerState initializes empty")
        void step2ComposerState() {
            var composer = new ComposerState();
            assertEquals("", composer.text());
            assertEquals(0, composer.cursor());
            assertEquals(0, composer.length());
            assertFalse(composer.slashActive());
            assertFalse(composer.historySearchActive());
        }

        @Test
        @DisplayName("step 3: CommandRegistry with BuiltinCommands registers 12 commands")
        void step3CommandRegistry() {
            var registry = new CommandRegistry();
            new BuiltinCommands(registry).registerAll();
            var commands = registry.list();
            assertEquals(12, commands.size());

            // Verify key commands exist
            var names = commands.stream()
                    .map(com.jay.tui.commands.CommandRegistry.CommandInfo::name)
                    .toList();
            assertTrue(names.contains("help"));
            assertTrue(names.contains("exit"));
            assertTrue(names.contains("model"));
            assertTrue(names.contains("clear"));
        }

        @Test
        @DisplayName("steps 4-5: EngineConfig → Engine.spawn → EngineHandle")
        void step4And5EngineSpawn() throws Exception {
            var config = EngineConfig.builder()
                    .model("test-model")
                    .workspace(Path.of("/tmp"))
                    .allowShell(false)
                    .showThinking(false)
                    .maxSteps(5)
                    .build();

            assertEquals("test-model", config.model());
            assertEquals(Path.of("/tmp"), config.workspace());
            assertFalse(config.allowShell());
            assertEquals(5, config.maxSteps());

            // Spawn engine
            EngineHandle handle = TuiEngine.spawn(config);
            assertNotNull(handle);
            assertTrue(handle.isRunning());

            // Verify EngineInitialized event
            Thread.sleep(200);
            var events = handle.drainEvents();
            assertTrue(events.stream().anyMatch(
                    e -> e instanceof TuiEngineEvent.EngineInitialized));

            handle.shutdown();
            Thread.sleep(300);
            assertFalse(handle.isRunning());
        }

        @Test
        @DisplayName("full 5-step init: AppState → Composer → Commands → Config → Engine")
        void fullInitializationChain() throws Exception {
            // Step 1
            var appState = new AppState();
            // Step 2
            var composer = new ComposerState();
            // Step 3
            var registry = new CommandRegistry();
            new BuiltinCommands(registry).registerAll();
            assertEquals(12, registry.list().size());

            // Step 4
            var config = TuiApplication.buildEngineConfig(Map.of(
                    "model", "deepseek-v4-pro", "maxSteps", "10"));
            assertEquals("deepseek-v4-pro", config.model());
            assertEquals(10, config.maxSteps());

            // Step 5
            EngineHandle handle = TuiEngine.spawn(config);
            assertTrue(handle.isRunning());

            handle.shutdown();
            Thread.sleep(300);
            assertFalse(handle.isRunning());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 5. EventContext Record
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("EventContext Record")
    class EventContextTests {

        @Test
        @DisplayName("EventContext bundles all state for event handling")
        void eventContextConstruction() {
            var appState = new AppState();
            var composer = new ComposerState();
            var registry = new CommandRegistry();
            var slashMenu = new SlashMenu(registry);
            var ctx = new TuiApplication.EventContext(
                    appState, composer, registry, slashMenu, null, null, null,
                    null, null, null, "test-model"
            );

            assertEquals(appState, ctx.appState());
            assertEquals(composer, ctx.composer());
            assertEquals("test-model", ctx.currentModel());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 6. Engine Integration — SendMessage Full Flow
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Engine SendMessage Integration")
    class SendMessageIntegration {

        @Test
        @DisplayName("SendMessage produces full turn lifecycle events")
        void sendMessageFullTurn() throws Exception {
            var config = EngineConfig.builder()
                    .model("test-model")
                    .workspace(Path.of("."))
                    .maxSteps(10)
                    .build();
            EngineHandle handle = TuiEngine.spawn(config);
            Thread.sleep(200);
            handle.drainEvents(); // clear init

            handle.sendOp(new TuiEngineOp.SendMessage(
                    "Hello world", "agent", "test-model",
                    true, false, false, false, List.of()));
            Thread.sleep(300);
            var events = handle.drainEvents();

            // Turn lifecycle must be present
            assertTrue(events.stream().anyMatch(
                    e -> e instanceof TuiEngineEvent.TurnStarted));
            assertTrue(events.stream().anyMatch(
                    e -> e instanceof TuiEngineEvent.MessageDelta));

            handle.shutdown();
        }

        @Test
        @DisplayName("SetModel op changes model and emits events")
        void setModelOp() throws Exception {
            var config = EngineConfig.builder().workspace(Path.of(".")).build();
            EngineHandle handle = TuiEngine.spawn(config);
            Thread.sleep(200);
            handle.drainEvents();

            handle.sendOp(new TuiEngineOp.SetModel("new-model", "agent"));
            Thread.sleep(200);
            var events = handle.drainEvents();

            assertTrue(events.stream().anyMatch(
                    e -> e instanceof TuiEngineEvent.ModelChanged mc
                            && "new-model".equals(mc.modelName())));
            assertTrue(events.stream().anyMatch(
                    e -> e instanceof TuiEngineEvent.SessionUpdated));

            handle.shutdown();
        }

        @Test
        @DisplayName("CancelRequest sets cancel flag")
        void cancelRequestOp() throws Exception {
            var config = EngineConfig.builder().workspace(Path.of(".")).build();
            EngineHandle handle = TuiEngine.spawn(config);
            Thread.sleep(200);
            handle.drainEvents();

            assertFalse(handle.isCancelled());
            handle.sendOp(new TuiEngineOp.CancelRequest());
            Thread.sleep(100);
            handle.drainEvents();
            assertTrue(handle.isCancelled());

            handle.shutdown();
        }

        @Test
        @DisplayName("ChangeMode emits status message")
        void changeModeOp() throws Exception {
            var config = EngineConfig.builder().workspace(Path.of(".")).build();
            EngineHandle handle = TuiEngine.spawn(config);
            Thread.sleep(200);
            handle.drainEvents();

            handle.sendOp(new TuiEngineOp.ChangeMode("plan"));
            Thread.sleep(200);
            var events = handle.drainEvents();

            assertTrue(events.stream().anyMatch(
                    e -> e instanceof TuiEngineEvent.StatusMessage s
                            && s.text().contains("plan")));

            handle.shutdown();
        }

        @Test
        @DisplayName("Shutdown terminates engine cleanly")
        void shutdownTerminates() throws Exception {
            var config = EngineConfig.builder().workspace(Path.of(".")).build();
            EngineHandle handle = TuiEngine.spawn(config);
            assertTrue(handle.isRunning());
            handle.shutdown();
            Thread.sleep(300);
            assertFalse(handle.isRunning());
        }

        @Test
        @DisplayName("CompactContext emits compaction lifecycle")
        void compactContextOp() throws Exception {
            var config = EngineConfig.builder().workspace(Path.of(".")).build();
            EngineHandle handle = TuiEngine.spawn(config);
            Thread.sleep(200);
            handle.drainEvents();

            handle.sendOp(new TuiEngineOp.CompactContext());
            Thread.sleep(200);
            var events = handle.drainEvents();

            assertTrue(events.stream().anyMatch(
                    e -> e instanceof TuiEngineEvent.CompactionStarted));
            assertTrue(events.stream().anyMatch(
                    e -> e instanceof TuiEngineEvent.CompactionCompleted));

            handle.shutdown();
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 7. TurnContext and Session (Core State)
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Core State: TurnContext & Session")
    class CoreStateTests {

        @Test
        @DisplayName("TurnContext tracks steps, tools, and cancellation")
        void turnContextFull() {
            var turn = new TurnContext(5);
            assertEquals(0, turn.step());
            assertFalse(turn.atMaxSteps());

            turn.nextStep(); // 1
            turn.nextStep(); // 2
            assertEquals(2, turn.step());

            turn.recordToolCall();
            assertTrue(turn.hasToolCalls());
            assertEquals(1, turn.toolCallCount());

            assertFalse(turn.isCancelled());
            turn.cancel();
            assertTrue(turn.isCancelled());
            turn.resetCancel();
            assertFalse(turn.isCancelled());

            turn.addUsage(200, 100);
            assertEquals(200, turn.inputTokens());
            assertEquals(100, turn.outputTokens());
        }

        @Test
        @DisplayName("TurnContext at max steps returns false from nextStep")
        void turnContextMaxSteps() {
            var turn = new TurnContext(2);
            assertTrue(turn.nextStep());  // step=1
            assertFalse(turn.atMaxSteps());
            assertFalse(turn.nextStep()); // step=2, no more
            assertTrue(turn.atMaxSteps());
        }

        @Test
        @DisplayName("Session mutableMessages shares same list")
        void sessionMutableMessages() {
            var session = new Session("m", Path.of("."), true, false);
            session.addMessage(com.jay.tui.client.ChatMessage.user("hello"));
            session.addMessage(com.jay.tui.client.ChatMessage.assistant("hi"));

            assertEquals(2, session.messages().size());
            assertEquals(2, session.mutableMessages().size());

            // Modifying mutable list affects session
            session.mutableMessages().add(
                    com.jay.tui.client.ChatMessage.user("another"));
            assertEquals(3, session.messages().size());
        }

        @Test
        @DisplayName("Session usage accumulates over multiple turns")
        void sessionUsageAccumulation() {
            var session = new Session("m", Path.of("."), true, false);
            session.addUsage(100, 50, 20L, 80L);
            session.addUsage(200, 100, null, 40L);

            assertEquals(300, session.totalUsage().inputTokens());
            assertEquals(150, session.totalUsage().outputTokens());
            assertEquals(450, session.totalUsage().totalTokens());
            assertEquals(20L, session.totalUsage().cacheCreationInputTokens());
            assertEquals(120L, session.totalUsage().cacheReadInputTokens());
        }

        @Test
        @DisplayName("ErrorTaxonomy classifies all categories")
        void errorTaxonomyComplete() {
            // Recoverable
            assertEquals(ErrorTaxonomy.Category.NETWORK,
                    ErrorTaxonomy.classify("Connection refused").category());
            assertEquals(ErrorTaxonomy.Category.TIMEOUT,
                    ErrorTaxonomy.classify("Request timed out").category());
            assertEquals(ErrorTaxonomy.Category.RATE_LIMIT,
                    ErrorTaxonomy.classify("429 rate limit exceeded").category());

            // Not recoverable
            assertEquals(ErrorTaxonomy.Category.AUTH,
                    ErrorTaxonomy.classify("401 unauthorized").category());
            assertEquals(ErrorTaxonomy.Category.PARSE,
                    ErrorTaxonomy.classify("JSON parse error").category());
            assertEquals(ErrorTaxonomy.Category.INVALID_INPUT,
                    ErrorTaxonomy.classify("invalid request").category());
            assertEquals(ErrorTaxonomy.Category.INTERNAL,
                    ErrorTaxonomy.classify("unknown").category());

            // shouldRetry
            assertTrue(ErrorTaxonomy.shouldRetry(ErrorTaxonomy.Category.NETWORK));
            assertTrue(ErrorTaxonomy.shouldRetry(ErrorTaxonomy.Category.TIMEOUT));
            assertTrue(ErrorTaxonomy.shouldRetry(ErrorTaxonomy.Category.RATE_LIMIT));
            assertFalse(ErrorTaxonomy.shouldRetry(ErrorTaxonomy.Category.AUTH));
            assertFalse(ErrorTaxonomy.shouldRetry(ErrorTaxonomy.Category.INTERNAL));
        }
    }
}
