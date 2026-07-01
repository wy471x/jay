package com.jay.tui;

import com.jay.tui.commands.BuiltinCommands;
import com.jay.tui.commands.CommandRegistry;
import com.jay.tui.core.EngineConfig;
import com.jay.tui.core.EngineHandle;
import com.jay.tui.core.TuiAction;
import com.jay.tui.core.TuiEngine;
import com.jay.tui.core.TuiEngineEvent;
import com.jay.tui.core.TuiEngineOp;
import com.jay.tui.core.compaction.CompactionConfig;
import com.jay.tui.input.CommandPalette;
import com.jay.tui.input.InputHandler;
import com.jay.tui.input.KeyDispatcher;
import com.jay.tui.input.SlashMenu;
import com.jay.tui.state.AppState;
import com.jay.tui.state.ComposerState;
import com.jay.tui.state.StatusBarState;
import com.jay.tui.views.HelpOverlay;
import com.jay.tui.views.ViewAction;
import com.jay.tui.views.ViewStack;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.TuiRunner;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.TickEvent;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * TUI entry point using TamboUI {@link TuiRunner} and the 7-step main path.
 *
 * <h3>7-Step Main Path</h3>
 * <ol>
 *   <li>Initialize {@link AppState} — centralized mutable state tree</li>
 *   <li>Initialize {@link ComposerState} — text buffer, cursor, history</li>
 *   <li>Build {@link CommandRegistry} + {@link BuiltinCommands} — 12 slash commands</li>
 *   <li>Build {@link EngineConfig} from CLI args / system properties</li>
 *   <li>Spawn {@link TuiEngine} on virtual thread, get {@link EngineHandle}</li>
 *   <li>Event loop — TamboUI {@code TuiRunner.run()} with event + render handlers</li>
 *   <li>Render — {@link TuiView#render} with engine event draining</li>
 * </ol>
 */
public final class TuiApplication {

    private static final String DEFAULT_MODEL = "deepseek-v4-pro";
    private static final String MODE_AGENT = "agent";

    private TuiApplication() { }

    /**
     * Start the TUI with the 7-step main path.
     *
     * @param args CLI arguments (--model, --workspace, --allow-shell, --no-shell,
     *             --show-thinking, --no-thinking, --max-steps, --compaction)
     */
    public static void start(String[] args) {
        // Step 1: Initialize AppState
        var appState = new AppState();

        // Step 2: Initialize ComposerState
        var composer = new ComposerState();

        // Step 3: Build CommandRegistry + BuiltinCommands
        var commandRegistry = new CommandRegistry();
        new BuiltinCommands(commandRegistry).registerAll();

        var slashMenu = new SlashMenu(commandRegistry);
        var commandPalette = new CommandPalette(commandRegistry);
        var viewStack = new ViewStack();
        var helpOverlay = new HelpOverlay(commandRegistry);
        var inputHandler = new InputHandler(composer);
        var view = new TuiView(appState, composer, slashMenu, commandRegistry);

        // Step 4: Build EngineConfig from CLI args / system properties
        var cliArgs = parseArgs(args);
        var engineConfig = buildEngineConfig(cliArgs);

        // Step 5: Spawn engine on virtual thread, get handle
        EngineHandle engineHandle = TuiEngine.spawn(engineConfig);
        appState.statusBar().setModelInfo(engineConfig.model(), null);

        var tuiConfig = TuiConfig.builder()
                .alternateScreen(true)
                .rawMode(true)
                .mouseCapture(true)
                .bracketedPaste(true)
                .build();

        // Wrap state into a context record to reduce parameter count
        var ctx = new EventContext(appState, composer, commandRegistry,
                slashMenu, commandPalette, viewStack, helpOverlay,
                inputHandler, view, engineHandle, engineConfig.model());

        // Step 6-7: Event loop + Render
        try (var runner = TuiRunner.create(tuiConfig)) {
            runner.run(
                (event, r) -> handleEvent(event, ctx),
                frame -> renderFrame(frame, ctx)
            );
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            engineHandle.shutdown();
        }
    }

    // ── Event context (bundles all mutable state) ───────────────────────

    record EventContext(
            AppState appState,
            ComposerState composer,
            CommandRegistry commandRegistry,
            SlashMenu slashMenu,
            CommandPalette commandPalette,
            ViewStack viewStack,
            HelpOverlay helpOverlay,
            InputHandler inputHandler,
            TuiView view,
            EngineHandle engineHandle,
            String currentModel
    ) {}

    // ── Step 6: Event handling ──────────────────────────────────────────

    private static boolean handleEvent(Object event, EventContext ctx) {
        if (event instanceof TickEvent) {
            ctx.view.tick();
            ctx.viewStack.tick();
            return !ctx.appState.shuttingDown();
        }
        if (event instanceof KeyEvent ke) {
            return handleKeyEvent(ke, ctx);
        }
        return !ctx.appState.shuttingDown();
    }

    @SuppressWarnings({"java:S3776", "java:MethodLength"})
    private static boolean handleKeyEvent(KeyEvent ke, EventContext ctx) {
        if (ke.isCtrlC()) {
            ctx.appState.apply(new com.jay.tui.core.TuiEvent.ShutdownRequested("quit"));
            return false;
        }

        char ch = ke.character();
        boolean ctrl = ke.hasCtrl();
        boolean alt = ke.hasAlt();
        KeyCode code = ke.code() != null ? ke.code() : KeyCode.UNKNOWN;

        // Modal stack input
        if (!ctx.viewStack.isEmpty()) {
            var action = ctx.viewStack.handleKey(ch, code, ctrl, alt);
            if (action instanceof ViewAction.Close) {
                ctx.viewStack.pop();
            }
            return !ctx.appState.shuttingDown();
        }

        // Command palette
        if (ctx.commandPalette.isVisible()) {
            return handleCommandPaletteKey(ch, code, ctrl, alt, ctx);
        }

        // Slash menu active
        if (ctx.composer.slashActive()) {
            return handleSlashMenuKey(ch, code, ctrl, alt, ctx);
        }

        // History search mode
        if (ctx.composer.historySearchActive()) {
            return handleHistorySearchKey(code, ch, ctx.composer);
        }

        // Global shortcuts
        if (handleGlobalShortcut(ch, code, ctrl, ctx)) {
            return true;
        }

        // Scroll keys
        if (handleScrollKeys(code, ctrl, ctx.appState)) {
            return true;
        }

        // Escape chain
        if (code == KeyCode.ESCAPE) {
            var escAction = ctx.composer.handleEscape();
            if (escAction == ComposerState.EscapeAction.CLOSE_SLASH_MENU) {
                ctx.slashMenu.updateFilter(ctx.appState);
            }
            return true;
        }

        // Normal input
        String text = ke.string();
        Optional<TuiAction> action;
        if (text != null && !text.isEmpty() && hasNonAscii(text)) {
            action = ctx.inputHandler.handleTextInput(text, ctrl, alt);
        } else {
            action = ctx.inputHandler.handleInput(code, ch, ctrl, alt);
        }
        if (action.isPresent()) {
            return dispatchAction(action.get(), ctx);
        }
        return !ctx.appState.shuttingDown();
    }

    // ── Key sub-handlers ────────────────────────────────────────────────

    private static boolean handleCommandPaletteKey(char ch, KeyCode code,
                                                    boolean ctrl, boolean alt,
                                                    EventContext ctx) {
        if (ctx.commandPalette.handleKey(ch, code, ctrl, alt)) {
            var sel = ctx.commandPalette.selectedEntry();
            if (sel != null && !ctx.commandPalette.isVisible()) {
                ctx.composer.setText("/" + sel.label() + " ");
                ctx.composer.commit();
                ctx.engineHandle.sendOp(new TuiEngineOp.SendMessage(
                        "/" + sel.label(), MODE_AGENT,
                        ctx.currentModel, true, false, false,
                        false, List.of()));
            }
        }
        return !ctx.appState.shuttingDown();
    }

    @SuppressWarnings("java:S3776")
    private static boolean handleSlashMenuKey(char ch, KeyCode code,
                                               boolean ctrl, boolean alt,
                                               EventContext ctx) {
        if (code == KeyCode.ENTER) {
            var action = ctx.slashMenu.getSelectedAction();
            if (action.isPresent() && action.get() instanceof TuiAction.ExecuteSlashCommand esc) {
                var result = ctx.commandRegistry.execute(esc.command(), esc.args());
                if (result instanceof com.jay.tui.commands.CommandResult.Exit) {
                    ctx.appState.apply(
                            new com.jay.tui.core.TuiEvent.ShutdownRequested("exit"));
                    return false;
                }
            }
            ctx.composer.setText("");
            return true;
        }
        if (code == KeyCode.ESCAPE) {
            ctx.composer.handleEscape();
            return true;
        }
        if (code == KeyCode.TAB) {
            ctx.slashMenu.selectNext();
            return true;
        }
        if (code == KeyCode.UP) {
            ctx.slashMenu.selectPrev();
            return true;
        }
        if (code == KeyCode.DOWN) {
            ctx.slashMenu.selectNext();
            return true;
        }
        if (code == KeyCode.BACKSPACE) {
            ctx.composer.deleteBefore();
            if (ctx.composer.text().isEmpty() || !ctx.composer.text().startsWith("/")) {
                ctx.composer.handleEscape();
            }
            ctx.slashMenu.updateFilter(ctx.appState);
            return true;
        }
        if (KeyDispatcher.isPrintable(ch) && !ctrl && !alt) {
            ctx.composer.insertChar(ch);
            ctx.slashMenu.updateFilter(ctx.appState);
            return true;
        }
        return true;
    }

    private static boolean handleHistorySearchKey(KeyCode code, char ch,
                                                   ComposerState composer) {
        if (code == KeyCode.ENTER) {
            composer.acceptHistorySearch();
        } else if (code == KeyCode.ESCAPE) {
            composer.cancelHistorySearch();
        } else if (code == KeyCode.BACKSPACE) {
            composer.historySearchBackspace();
        } else if (ch >= 32 && !Character.isISOControl(ch)) {
            composer.historySearchAppend(ch);
        }
        return true;
    }

    private static boolean handleGlobalShortcut(char ch, KeyCode code,
                                                 boolean ctrl, EventContext ctx) {
        if (ctrl && (ch == 'p' || ch == 'P')) {
            ctx.commandPalette.show();
            return true;
        }
        if (ctrl && (ch == 'r' || ch == 'R')) {
            ctx.composer.startHistorySearch();
            return true;
        }
        if (ctrl && (ch == 'b' || ch == 'B')) {
            ctx.appState.sidebar().toggle();
            return true;
        }
        if (code == KeyCode.F1) {
            ctx.viewStack.push(ctx.helpOverlay);
            return true;
        }
        return false;
    }

    private static boolean handleScrollKeys(KeyCode code, boolean ctrl,
                                             AppState appState) {
        if (ctrl && code == KeyCode.UNKNOWN) {
            return false;
        }
        return switch (code) {
            case UP -> {
                appState.viewport().scrollUp(1);
                yield true;
            }
            case DOWN -> {
                appState.viewport().scrollDown(1);
                yield true;
            }
            case PAGE_UP -> {
                int h = Math.max(1, appState.viewport().viewportHeight());
                appState.viewport().scrollUp(h / 2);
                yield true;
            }
            case PAGE_DOWN -> {
                int h = Math.max(1, appState.viewport().viewportHeight());
                appState.viewport().scrollDown(h / 2);
                yield true;
            }
            case HOME -> {
                appState.viewport().scrollToTop();
                yield true;
            }
            case END -> {
                appState.viewport().scrollToBottom();
                yield true;
            }
            default -> false;
        };
    }

    // ── Action dispatch ─────────────────────────────────────────────────

    private static boolean dispatchAction(TuiAction act, EventContext ctx) {
        return switch (act) {
            case TuiAction.Quit q -> {
                ctx.engineHandle.shutdown();
                yield false;
            }
            case TuiAction.SendMessage m -> {
                sendUserMessage(m.text(), ctx);
                yield true;
            }
            case TuiAction.CancelResponse cr -> {
                ctx.engineHandle.sendOp(new TuiEngineOp.CancelRequest());
                yield true;
            }
            case TuiAction.ScrollUp su -> {
                ctx.appState.viewport().scrollUp(su.lines());
                yield true;
            }
            case TuiAction.ScrollDown sd -> {
                ctx.appState.viewport().scrollDown(sd.lines());
                yield true;
            }
            case TuiAction.ScrollToTop st -> {
                ctx.appState.viewport().scrollToTop();
                yield true;
            }
            case TuiAction.ScrollToBottom sb -> {
                ctx.appState.viewport().scrollToBottom();
                yield true;
            }
            case TuiAction.ToggleSidebar ts -> {
                ctx.appState.sidebar().toggle();
                yield true;
            }
            case TuiAction.OpenSlashMenu osm -> {
                ctx.composer.insertChar('/');
                yield true;
            }
            case TuiAction.SwitchModel sm -> {
                ctx.engineHandle.sendOp(
                        new TuiEngineOp.SetModel(sm.modelName(), MODE_AGENT));
                yield true;
            }
            case TuiAction.SwitchProvider sp -> {
                ctx.appState.statusBar().setStatus(
                        "Provider: " + sp.provider(),
                        StatusBarState.Severity.INFO);
                yield true;
            }
            default -> true;
        };
    }

    private static void sendUserMessage(String text, EventContext ctx) {
        ctx.engineHandle.sendOp(new TuiEngineOp.SendMessage(
                text, MODE_AGENT, ctx.currentModel,
                true, false, false, false, List.of()));
        ctx.view.appendTranscript("**> " + text + "**");
        ctx.view.appendTranscript("");
        ctx.composer.clear();
    }

    // ── Step 7: Render ──────────────────────────────────────────────────

    private static void renderFrame(Object frame, EventContext ctx) {
        var f = (dev.tamboui.terminal.Frame) frame;
        for (var engineEvent : ctx.engineHandle.drainEvents()) {
            applyEngineEvent(ctx.appState, ctx.view, engineEvent);
        }
        ctx.slashMenu.updateFilter(ctx.appState);
        ctx.view.render(f);

        if (!ctx.viewStack.isEmpty()) {
            ctx.viewStack.render(f, f.area());
        }
        if (ctx.commandPalette.isVisible()) {
            ctx.commandPalette.render(f, f.area());
        }
    }

    // ── Engine event → AppState + transcript ───────────────────────────

    /**
     * Translate {@link TuiEngineEvent} into {@link AppState} mutations
     * and transcript updates. Handles all 28 event variants.
     */
    static void applyEngineEvent(AppState appState, TuiView view,
                                  TuiEngineEvent event) {
        switch (event) {
            // Streaming
            case TuiEngineEvent.MessageStarted s ->
                    view.appendTranscript("");
            case TuiEngineEvent.MessageDelta d ->
                    view.appendTranscript("  " + d.content());
            case TuiEngineEvent.MessageComplete c ->
                    view.appendTranscript("");

            // Thinking
            case TuiEngineEvent.ThinkingStarted ts ->
                    appState.statusBar().setStatus("Thinking...",
                            StatusBarState.Severity.INFO);
            case TuiEngineEvent.ThinkingDelta td ->
                    view.appendTranscript("  [thinking] " + td.content());
            case TuiEngineEvent.ThinkingComplete tc ->
                    appState.statusBar().setStatus("Ready",
                            StatusBarState.Severity.INFO);

            // Tool events
            case TuiEngineEvent.ToolCallStarted t ->
                    view.appendTranscript("  [tool:" + t.name() + "]");
            case TuiEngineEvent.ToolCallComplete t -> {
                if (t.result().success()) {
                    view.appendTranscript("  [tool:" + t.name() + " OK]");
                } else {
                    view.appendTranscript("  [tool:" + t.name() + " ERR: "
                            + t.result().error() + "]");
                }
            }
            case TuiEngineEvent.ApprovalRequired a ->
                    view.appendTranscript("  [approval needed: " + a.toolName()
                            + " — " + a.description() + "]");

            // Turn lifecycle
            case TuiEngineEvent.TurnStarted ts ->
                    view.appendTranscript("--- Turn " + ts.turnId());
            case TuiEngineEvent.TurnComplete tc ->
                    view.appendTranscript("--- Turn complete");
            case TuiEngineEvent.TurnAborted ta ->
                    view.appendTranscript("--- Turn aborted: " + ta.reason());

            // Compaction & purging
            case TuiEngineEvent.CompactionStarted cs ->
                    appState.statusBar().setStatus("Compacting: " + cs.message(),
                            StatusBarState.Severity.INFO);
            case TuiEngineEvent.CompactionCompleted cc ->
                    appState.statusBar().setStatus("Compacted: "
                            + cc.messagesBefore() + "→" + cc.messagesAfter(),
                            StatusBarState.Severity.INFO);
            case TuiEngineEvent.CompactionFailed cf ->
                    appState.statusBar().setStatus("Compaction failed: "
                            + cf.message(), StatusBarState.Severity.ERROR);
            case TuiEngineEvent.PurgeStarted ps ->
                    appState.statusBar().setStatus("Purging: " + ps.message(),
                            StatusBarState.Severity.INFO);
            case TuiEngineEvent.PurgeCompleted pc ->
                    appState.statusBar().setStatus("Purged: " + pc.removedCount()
                            + " removed, " + pc.replacedCount() + " replaced",
                            StatusBarState.Severity.INFO);
            case TuiEngineEvent.PurgeFailed pf ->
                    appState.statusBar().setStatus("Purge failed: "
                            + pf.message(), StatusBarState.Severity.ERROR);

            // System events
            case TuiEngineEvent.StatusMessage s ->
                    appState.statusBar().setStatus(s.text(),
                            StatusBarState.Severity.INFO);
            case TuiEngineEvent.EngineError e ->
                    appState.statusBar().setStatus(e.message(),
                            StatusBarState.Severity.ERROR);
            case TuiEngineEvent.EngineInitialized i ->
                    appState.statusBar().setModelInfo(i.model(), null);
            case TuiEngineEvent.SessionUpdated su ->
                    appState.statusBar().setStatus(su.messageCount()
                            + " msgs, " + su.tokensUsed() + " tokens",
                            StatusBarState.Severity.INFO);
            case TuiEngineEvent.PrefixCacheChange pcc ->
                    appState.statusBar().setStatus("Cache: "
                            + pcc.description(), StatusBarState.Severity.INFO);
            case TuiEngineEvent.ModelChanged m ->
                    appState.statusBar().setModelInfo(
                            m.modelName(), m.provider());

            // I/O events
            case TuiEngineEvent.UserInputRequired ui ->
                    appState.statusBar().setStatus("Input needed: "
                            + ui.prompt(), StatusBarState.Severity.INFO);
            case TuiEngineEvent.PauseEvents pe ->
                    appState.statusBar().setStatus("Input paused",
                            StatusBarState.Severity.INFO);
            case TuiEngineEvent.ResumeEvents re ->
                    appState.statusBar().setStatus("Ready",
                            StatusBarState.Severity.INFO);

            // Seam events
            case TuiEngineEvent.SeamProduced sp ->
                    appState.statusBar().setStatus(
                            "Seam L" + sp.level() + " produced (~"
                                    + sp.tokenEstimate() + " tokens)",
                            StatusBarState.Severity.INFO);
            case TuiEngineEvent.SeamRecompacted sr ->
                    appState.statusBar().setStatus(
                            "Seam L" + sr.level() + " recompacted ("
                                    + sr.priorSeamCount() + " prior)",
                            StatusBarState.Severity.INFO);
        }
    }

    // ── CLI arg parsing ─────────────────────────────────────────────────

    /**
     * Parse CLI arguments into a key-value map.
     *
     * <p>Supported flags:
     * <ul>
     *   <li>{@code --model <name>} — model ID (default: deepseek-v4-pro)</li>
     *   <li>{@code --workspace <path>} — working directory (default: .)</li>
     *   <li>{@code --allow-shell} / {@code --no-shell} — shell tools toggle</li>
     *   <li>{@code --show-thinking} / {@code --no-thinking} — thinking display</li>
     *   <li>{@code --max-steps <n>} — max tool steps per turn</li>
     *   <li>{@code --compaction} — enable context compaction</li>
     *   <li>{@code --trust} — enable trust mode</li>
     *   <li>{@code --no-memory} — disable memory system</li>
     * </ul>
     */
    static Map<String, String> parseArgs(String[] args) {
        Map<String, String> result = new HashMap<>();
        if (args == null) return result;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--model" -> result.put("model", nextArg(args, i));
                case "--workspace" -> result.put("workspace", nextArg(args, i));
                case "--max-steps" -> result.put("maxSteps", nextArg(args, i));
                case "--allow-shell" -> result.put("allowShell", "true");
                case "--no-shell" -> result.put("allowShell", "false");
                case "--show-thinking" -> result.put("showThinking", "true");
                case "--no-thinking" -> result.put("showThinking", "false");
                case "--compaction" -> result.put("compaction", "true");
                case "--trust" -> result.put("trustMode", "true");
                case "--no-memory" -> result.put("memoryEnabled", "false");
                default -> {
                    if (!arg.startsWith("-")) {
                        result.putIfAbsent("positional", arg);
                    }
                }
            }
        }
        return result;
    }

    private static String nextArg(String[] args, int idx) {
        if (idx + 1 < args.length && !args[idx + 1].startsWith("-")) {
            return args[idx + 1];
        }
        return "";
    }

    /** Build {@link EngineConfig} from parsed CLI args. */
    static EngineConfig buildEngineConfig(Map<String, String> args) {
        var builder = EngineConfig.builder();

        if (args.containsKey("model")) {
            builder.model(args.get("model"));
        }
        if (args.containsKey("workspace")) {
            builder.workspace(Path.of(args.get("workspace")));
        }
        if (args.containsKey("allowShell")) {
            builder.allowShell(Boolean.parseBoolean(args.get("allowShell")));
        }
        if (args.containsKey("showThinking")) {
            builder.showThinking(Boolean.parseBoolean(args.get("showThinking")));
        }
        if (args.containsKey("maxSteps")) {
            try {
                builder.maxSteps(Integer.parseInt(args.get("maxSteps")));
            } catch (NumberFormatException ignored) {
                // keep default
            }
        }
        if ("true".equals(args.get("compaction"))) {
            builder.compaction(CompactionConfig.of(true, 4000,
                    args.getOrDefault("model", DEFAULT_MODEL)));
        }
        if ("true".equals(args.get("trustMode"))) {
            builder.trustMode(true);
        }
        if ("false".equals(args.get("memoryEnabled"))) {
            builder.memoryEnabled(false);
        }

        // System property overrides
        String sysModel = System.getProperty("jay.model");
        if (sysModel != null) {
            builder.model(sysModel);
        }
        String sysWorkspace = System.getProperty("jay.workspace");
        if (sysWorkspace != null) {
            builder.workspace(Path.of(sysWorkspace));
        }

        return builder.build();
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private static boolean hasNonAscii(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) > 127) return true;
        }
        return false;
    }

    public static void main(String[] args) {
        start(args);
    }
}
