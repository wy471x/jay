package com.jay.tui.input;

import com.jay.tui.commands.CommandRegistry;
import com.jay.tui.core.TuiAction;
import com.jay.tui.state.AppState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Slash command overlay. Activates when composer buffer starts with '/'.
 * Maintains a filtered list of available commands based on typed text.
 */
public class SlashMenu {

    private final CommandRegistry registry;
    private int selectedIndex;
    private final List<String> filteredCommands = new ArrayList<>();

    public SlashMenu(CommandRegistry registry) {
        this.registry = registry;
    }

    /** Update filtered list based on current composer text. */
    public void updateFilter(AppState state) {
        var composer = state.composer();
        if (!composer.slashActive()) {
            filteredCommands.clear();
            selectedIndex = 0;
            return;
        }
        String filter = composer.slashFilter().toLowerCase();
        filteredCommands.clear();
        for (var cmd : registry.list()) {
            if (filter.isEmpty() || cmd.name().toLowerCase().contains(filter)) {
                filteredCommands.add(cmd.name());
            }
        }
        if (selectedIndex >= filteredCommands.size()) {
            selectedIndex = Math.max(0, filteredCommands.size() - 1);
        }
    }

    /** Get the list of currently matching command names. */
    public List<String> getFilteredCommands() {
        return List.copyOf(filteredCommands);
    }

    public int selectedIndex() {
        return selectedIndex;
    }

    public void selectNext() {
        if (!filteredCommands.isEmpty()) {
            selectedIndex = (selectedIndex + 1) % filteredCommands.size();
        }
    }

    public void selectPrev() {
        if (!filteredCommands.isEmpty()) {
            selectedIndex = (selectedIndex - 1 + filteredCommands.size())
                    % filteredCommands.size();
        }
    }

    /** Get the currently selected command action. */
    public Optional<TuiAction> getSelectedAction() {
        if (filteredCommands.isEmpty()) return Optional.empty();
        String cmd = filteredCommands.get(selectedIndex);
        return Optional.of(new TuiAction.ExecuteSlashCommand(cmd, List.of()));
    }

    public boolean isActive() {
        return !filteredCommands.isEmpty();
    }
}
