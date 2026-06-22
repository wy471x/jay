package com.jay.tui.core;

import com.jay.agent.ProviderKind;

import java.util.List;

/**
 * Actions flowing from user input down to the application engine.
 * Produced by InputHandler, consumed by TuiEngine on its virtual thread.
 *
 * <p>Equivalent to Rust's AppAction enum.
 * Actions that only affect the view (scroll by a few lines, toggle sidebar)
 * are render-local and never reach the engine dispatch.
 */
public sealed interface TuiAction {

    /** Gracefully quit the TUI. */
    record Quit() implements TuiAction {}

    /** Submit the current composer text as a user message. */
    record SendMessage(String text) implements TuiAction {}

    /** Cancel the current response / dismiss a popup. */
    record CancelResponse() implements TuiAction {}

    /** Scroll the transcript up by the given number of lines. */
    record ScrollUp(int lines) implements TuiAction {}

    /** Scroll the transcript down by the given number of lines. */
    record ScrollDown(int lines) implements TuiAction {}

    /** Scroll the transcript to the top. */
    record ScrollToTop() implements TuiAction {}

    /** Scroll the transcript to the bottom (follow mode). */
    record ScrollToBottom() implements TuiAction {}

    /** Switch to a different thread by ID. */
    record SwitchThread(String threadId) implements TuiAction {}

    /** Switch to a different provider. */
    record SwitchProvider(ProviderKind provider) implements TuiAction {}

    /** Switch to a different model by name. */
    record SwitchModel(String modelName) implements TuiAction {}

    /** Toggle the sidebar visibility. */
    record ToggleSidebar() implements TuiAction {}

    /** Open the slash command menu (/ mode). */
    record OpenSlashMenu() implements TuiAction {}

    /** Execute a slash command after Enter is pressed in slash mode. */
    record ExecuteSlashCommand(String command, List<String> args) implements TuiAction {}

    /** Copy the given text to clipboard. */
    record CopySelection(String text) implements TuiAction {}

    /** Terminal was resized. */
    record Resize(int columns, int rows) implements TuiAction {}
}
