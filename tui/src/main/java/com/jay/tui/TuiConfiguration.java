package com.jay.tui;

import com.jay.tui.commands.BuiltinCommands;
import com.jay.tui.commands.CommandRegistry;
import com.jay.tui.core.EngineConfig;
import com.jay.tui.core.EngineHandle;
import com.jay.tui.core.Session;
import com.jay.tui.input.InputHandler;
import com.jay.tui.input.SlashMenu;
import com.jay.tui.state.AppState;
import com.jay.tui.state.ComposerState;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

/**
 * Spring configuration for the TUI module.
 * Wires beans for the 7-step main path when running in CLI/Spring mode.
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
    EngineConfig engineConfig(TuiProperties props) {
        return EngineConfig.builder()
                .model("deepseek-v4-pro")
                .workspace(Path.of("."))
                .allowShell(true)
                .showThinking(true)
                .maxSteps(100)
                .build();
    }

    @Bean
    Session session(EngineConfig config) {
        return new Session(
                config.model(), config.workspace(),
                config.allowShell(), config.trustMode()
        );
    }

    /** Spawn the engine and return its handle. */
    @Bean(destroyMethod = "shutdown")
    EngineHandle engineHandle(EngineConfig config) {
        var engine = new com.jay.tui.core.TuiEngine(config);
        return engine.spawn();
    }
}
