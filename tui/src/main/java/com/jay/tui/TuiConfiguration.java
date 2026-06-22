package com.jay.tui;

import com.jay.tui.commands.BuiltinCommands;
import com.jay.tui.commands.CommandRegistry;
import com.jay.tui.core.TuiEngine;
import com.jay.tui.input.InputHandler;
import com.jay.tui.input.SlashMenu;
import com.jay.tui.state.AppState;
import com.jay.tui.state.ComposerState;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for the TUI module.
 * Wires all TUI beans. When Spring is available (CLI mode),
 * this provides dependency injection; for standalone TUI mode,
 * TuiApplication wires components manually.
 */
@Configuration
@EnableConfigurationProperties(TuiProperties.class)
public class TuiConfiguration {

    @Bean
    AppState appState() { return new AppState(); }

    @Bean
    ComposerState composerState() { return new ComposerState(); }

    @Bean
    CommandRegistry commandRegistry() { return new CommandRegistry(); }

    @Bean
    BuiltinCommands builtinCommands(CommandRegistry registry) {
        var cmds = new BuiltinCommands(registry);
        cmds.registerAll();
        return cmds;
    }

    @Bean
    SlashMenu slashMenu(CommandRegistry registry) { return new SlashMenu(registry); }

    @Bean
    InputHandler inputHandler(ComposerState composer) { return new InputHandler(composer); }

    @Bean
    TuiEngine tuiEngine() { return new TuiEngine(null); }
}
