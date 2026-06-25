package com.jay.tui;

import com.jay.tui.commands.BuiltinCommands;
import com.jay.tui.commands.CommandRegistry;
import com.jay.tui.core.EngineConfig;
import com.jay.tui.core.EngineHandle;
import com.jay.tui.core.TuiAction;
import com.jay.tui.core.TuiEngine;
import com.jay.tui.core.TuiEngineEvent;
import com.jay.tui.core.TuiEngineOp;
import com.jay.tui.input.CommandPalette;
import com.jay.tui.input.InputHandler;
import com.jay.tui.input.SlashMenu;
import com.jay.tui.state.AppState;
import com.jay.tui.state.ComposerState;
import com.jay.tui.views.HelpOverlay;
import com.jay.tui.views.ViewAction;
import com.jay.tui.views.ViewStack;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.TuiRunner;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.TickEvent;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * TUI entry point using TamboUI {@link TuiRunner} and the 7-step main path.
 *
 * <p>Wires: CLI args -> EngineConfig -> Engine.spawn() -> EngineHandle ->
 * event loop -> AppState.apply() -> TuiView.render().
 */
public final class TuiApplication {

    private TuiApplication() { }

    public static void start(String[] args) {
        var appState = new AppState();
        var composer = new ComposerState();
        var commandRegistry = new CommandRegistry();
        new BuiltinCommands(commandRegistry).registerAll();

        var slashMenu = new SlashMenu(commandRegistry);
        var commandPalette = new CommandPalette(commandRegistry);
        var viewStack = new ViewStack();
        var helpOverlay = new HelpOverlay(commandRegistry);
        var inputHandler = new InputHandler(composer);
        var view = new TuiView(appState, composer, slashMenu, commandRegistry);

        // Step 4: Build EngineConfig from CLI args / properties
        var engineConfig = EngineConfig.builder()
                .model("deepseek-v4-pro")
                .workspace(Path.of("."))
                .allowShell(true)
                .showThinking(true)
                .build();

        // Step 5: Spawn engine on virtual thread, get handle
        EngineHandle engineHandle = TuiEngine.spawn(engineConfig);

        var tuiConfig = TuiConfig.builder()
                .alternateScreen(true)
                .rawMode(true)
                .mouseCapture(true)
                .bracketedPaste(true)
                .build();

        try (var runner = TuiRunner.create(tuiConfig)) {
            runner.run(
                // ── Event handler ────────────────────────────────
                (event, r) -> {
                    if (event instanceof TickEvent) {
                        view.tick();
                        viewStack.tick();
                        return !appState.shuttingDown();
                    }
                    if (event instanceof KeyEvent keyEvent) {
                        if (keyEvent.isCtrlC()) {
                            appState.apply(new com.jay.tui.core.TuiEvent.ShutdownRequested("quit"));
                            return false;
                        }
                        String text = keyEvent.string();
                        char ch = keyEvent.character();
                        boolean ctrl = keyEvent.hasCtrl();
                        boolean alt = keyEvent.hasAlt();
                        KeyCode code = keyEvent.code() != null
                                ? keyEvent.code() : KeyCode.UNKNOWN;

                        // ── Modal stack input ──────────────────
                        if (!viewStack.isEmpty()) {
                            var action = viewStack.handleKey(ch, code, ctrl, alt);
                            if (action instanceof ViewAction.Close) {
                                viewStack.pop();
                            }
                            return !appState.shuttingDown();
                        }

                        // ── Command palette ────────────────────
                        if (commandPalette.isVisible()) {
                            if (commandPalette.handleKey(ch, code, ctrl, alt)) {
                                var sel = commandPalette.selectedEntry();
                                if (sel != null && !commandPalette.isVisible()) {
                                    composer.setText("/" + sel.label() + " ");
                                    composer.commit();
                                    engineHandle.sendOp(new TuiEngineOp.SendMessage(
                                            "/" + sel.label(), "agent",
                                            "deepseek-v4-pro", true, false, false,
                                            false, List.of()));
                                }
                            }
                            return !appState.shuttingDown();
                        }

                        // ── Global shortcuts ───────────────────
                        if (ctrl && ch == 'p') {   // Ctrl+P → palette
                            commandPalette.show();
                            return true;
                        }
                        if (ctrl && (ch == 'r' || ch == 'R')) { // Ctrl+R → history search
                            composer.startHistorySearch();
                            return true;
                        }
                        if (code == KeyCode.F1) {  // F1 → help
                            viewStack.push(helpOverlay);
                            return true;
                        }

                        // ── Escape chain ───────────────────────
                        if (code == KeyCode.ESCAPE) {
                            var escAction = composer.handleEscape();
                            if (escAction == ComposerState.EscapeAction.CLOSE_SLASH_MENU) {
                                slashMenu.updateFilter(appState);
                                return true;
                            }
                            if (escAction == ComposerState.EscapeAction.DISCARD_DRAFT) {
                                return true;
                            }
                            return true;
                        }

                        // ── History search mode ────────────────
                        if (composer.historySearchActive()) {
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

                        // ── Normal input ────────────────────────
                        Optional<com.jay.tui.core.TuiAction> action;
                        // IME / non-ASCII text → use string() to get full composed result
                        // ASCII input → use character() to avoid IME compose-char leakage
                        if (text != null && !text.isEmpty() && hasNonAscii(text)) {
                            action = inputHandler.handleTextInput(text, ctrl, alt);
                        } else {
                            action = inputHandler.handleInput(code, ch, ctrl, alt);
                        }
                        if (action.isPresent()) {
                            var act = action.get();
                            if (act instanceof TuiAction.Quit q) {
                                engineHandle.shutdown();
                                return false;
                            } else if (act instanceof TuiAction.SendMessage m) {
                                engineHandle.sendOp(new TuiEngineOp.SendMessage(
                                        m.text(), "agent",
                                        "deepseek-v4-pro", true, false, false,
                                        false, List.of()));
                                view.appendTranscript("**> " + m.text() + "**");
                                view.appendTranscript("");
                            }
                            // render-local actions (scroll, sidebar) ignored
                        }
                    }
                    return !appState.shuttingDown();
                },
                // ── Render handler ──────────────────────────────
                frame -> {
                    // Drain engine events and apply to app state
                    for (var engineEvent : engineHandle.drainEvents()) {
                        applyEngineEvent(appState, view, engineEvent);
                    }
                    slashMenu.updateFilter(appState);
                    view.render(frame);

                    if (!viewStack.isEmpty()) {
                        viewStack.render(frame, frame.area());
                    }
                    if (commandPalette.isVisible()) {
                        commandPalette.render(frame, frame.area());
                    }
                }
            );
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            engineHandle.shutdown();
        }
    }

    /**
     * Translate {@link TuiEngineEvent} into {@link AppState} mutations
     * and transcript updates.
     */
    private static void applyEngineEvent(AppState appState, TuiView view,
                                          TuiEngineEvent event) {
        switch (event) {
            case TuiEngineEvent.MessageDelta d ->
                    view.appendTranscript("  " + d.content());
            case TuiEngineEvent.MessageComplete c ->
                    view.appendTranscript("");
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
            case TuiEngineEvent.TurnStarted ts ->
                    view.appendTranscript("\u2500\u2500\u2500 Turn " + ts.turnId());
            case TuiEngineEvent.TurnComplete tc ->
                    view.appendTranscript("\u2500\u2500\u2500 Turn complete");
            case TuiEngineEvent.StatusMessage s ->
                    appState.statusBar().setStatus(s.text(),
                            com.jay.tui.state.StatusBarState.Severity.INFO);
            case TuiEngineEvent.EngineError e ->
                    appState.statusBar().setStatus(e.message(),
                            com.jay.tui.state.StatusBarState.Severity.ERROR);
            case TuiEngineEvent.EngineInitialized i ->
                    appState.statusBar().setModelInfo(i.model(), null);
            case TuiEngineEvent.ModelChanged m ->
                    appState.statusBar().setModelInfo(m.modelName(), m.provider());
            default -> {} // other events handled later
        }
    }

    /** Whether a string contains any non-ASCII character (IME result). */
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
