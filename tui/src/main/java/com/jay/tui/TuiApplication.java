package com.jay.tui;

import com.jay.tui.commands.BuiltinCommands;
import com.jay.tui.commands.CommandRegistry;
import com.jay.tui.core.TuiAction;
import com.jay.tui.core.TuiEngine;
import com.jay.tui.core.TuiEvent;
import com.jay.tui.input.CommandPalette;
import com.jay.tui.input.InputHandler;
import com.jay.tui.input.SlashMenu;
import com.jay.tui.state.AppState;
import com.jay.tui.state.ComposerState;
import com.jay.tui.views.HelpOverlay;
import com.jay.tui.views.ViewAction;
import com.jay.tui.views.ViewStack;
import dev.tamboui.tui.TuiRunner;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.TickEvent;

/**
 * TUI entry point using TamboUI {@link TuiRunner}.
 *
 * <p>Wires together the renderer, event handler, modal view stack,
 * command palette, help overlay, and slash command menu.
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
        var engine = new TuiEngine(null);
        var view = new TuiView(appState, composer, slashMenu, commandRegistry);

        engine.start();
        engine.onEngineReady();

        try (var runner = TuiRunner.create()) {
            runner.run(
                (event, r) -> {
                    if (event instanceof TickEvent) {
                        view.tick();
                        viewStack.tick();
                        return !appState.shuttingDown();
                    }
                    if (event instanceof KeyEvent keyEvent) {
                        if (keyEvent.isCtrlC()) {
                            appState.apply(new TuiEvent.ShutdownRequested("quit"));
                            return false;
                        }
                        char ch = keyEvent.character();
                        boolean ctrl = keyEvent.hasCtrl();
                        boolean alt = keyEvent.hasAlt();
                        KeyCode code = keyEvent.code() != null
                                ? keyEvent.code() : KeyCode.UNKNOWN;

                        // ── Modal stack input ──────────────────────
                        if (!viewStack.isEmpty()) {
                            var action = viewStack.handleKey(ch, code, ctrl, alt);
                            if (action instanceof ViewAction.Close) {
                                viewStack.pop();
                            }
                            return !appState.shuttingDown();
                        }

                        // ── Command palette ────────────────────────
                        if (commandPalette.isVisible()) {
                            if (commandPalette.handleKey(ch, code, ctrl, alt)) {
                                var sel = commandPalette.selectedEntry();
                                if (sel != null && !commandPalette.isVisible()) {
                                    composer.setText("/" + sel.label() + " ");
                                    composer.commit();
                                    engine.submitAction(new TuiAction.ExecuteSlashCommand(
                                            sel.label(), java.util.List.of()));
                                }
                            }
                            return !appState.shuttingDown();
                        }

                        // ── Global shortcuts ───────────────────────
                        if (ctrl && ch == 'p') {  // Ctrl+P → palette
                            commandPalette.show();
                            return true;
                        }
                        if (ctrl && (ch == 'r' || ch == 'R')) {  // Ctrl+R → history search
                            composer.startHistorySearch();
                            return true;
                        }
                        if (code == KeyCode.F1) {  // F1 → help
                            viewStack.push(helpOverlay);
                            return true;
                        }

                        // ── Escape chain ───────────────────────────
                        if (code == KeyCode.ESCAPE) { // Esc
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

                        // ── History search mode ────────────────────
                        if (composer.historySearchActive()) {
                            if (code == KeyCode.ENTER) {
                                composer.acceptHistorySearch();
                            } else if (code == KeyCode.ESCAPE) {
                                composer.cancelHistorySearch();
                            } else if (code == KeyCode.BACKSPACE) {
                                composer.historySearchBackspace();
                            } else if (ch >= 32 && ch < 127) {
                                composer.historySearchAppend(ch);
                            }
                            return true;
                        }

                        // ── Normal input ────────────────────────────
                        var action = inputHandler.handleInput(code, ch, ctrl, alt);
                        if (action.isPresent()) {
                            var act = action.get();
                            switch (act) {
                                case TuiAction.Quit q -> {
                                    appState.apply(new TuiEvent.ShutdownRequested("quit"));
                                    return false;
                                }
                                case TuiAction.SendMessage m -> {
                                    engine.submitAction(act);
                                    view.appendTranscript("**> " + m.text() + "**");
                                    view.appendTranscript("");
                                }
                                default -> engine.submitAction(act);
                            }
                        }
                    }
                    return !appState.shuttingDown();
                },
                frame -> {
                    for (var evt : engine.drainEvents()) {
                        appState.apply(evt);
                    }
                    slashMenu.updateFilter(appState);
                    view.render(frame);

                    // Render modal stack on top
                    if (!viewStack.isEmpty()) {
                        viewStack.render(frame, frame.area());
                    }
                    // Render command palette
                    if (commandPalette.isVisible()) {
                        commandPalette.render(frame, frame.area());
                    }
                }
            );
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            engine.shutdown();
        }
    }

    public static void main(String[] args) {
        start(args);
    }
}
