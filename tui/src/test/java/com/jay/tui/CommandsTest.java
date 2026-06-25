package com.jay.tui;

import com.jay.tui.commands.BuiltinCommands;
import com.jay.tui.commands.CommandRegistry;
import com.jay.tui.commands.CommandResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CommandsTest {

    // ── CommandResult ──────────────────────────────────────────────────

    @Nested
    class CommandResultTests {

        @Test
        void successCarriesMessage() {
            var r = new CommandResult.Success("done");
            assertEquals("done", r.message());
            assertInstanceOf(CommandResult.class, r);
        }

        @Test
        void errorCarriesMessage() {
            var r = new CommandResult.Error("failed");
            assertEquals("failed", r.message());
            assertInstanceOf(CommandResult.class, r);
        }

        @Test
        void exitHasNoData() {
            var r = new CommandResult.Exit();
            assertInstanceOf(CommandResult.class, r);
        }
    }

    // ── CommandRegistry ────────────────────────────────────────────────

    @Nested
    class CommandRegistryTests {

        private CommandRegistry registry;

        @BeforeEach
        void setUp() {
            registry = new CommandRegistry();
        }

        @Test
        void registerAndExecute() {
            registry.register("greet", "Say hello", "/greet [name]",
                    args -> new CommandResult.Success("Hello " + String.join(" ", args)));
            var result = registry.execute("greet", List.of("World"));
            assertInstanceOf(CommandResult.Success.class, result);
            assertEquals("Hello World", ((CommandResult.Success) result).message());
        }

        @Test
        void executeUnknownCommandReturnsError() {
            var result = registry.execute("no-such-cmd", List.of());
            assertInstanceOf(CommandResult.Error.class, result);
            assertTrue(((CommandResult.Error) result).message().contains("Unknown command"));
        }

        @Test
        void executeHandlesHandlerException() {
            registry.register("bad", "Throws", "/bad",
                    args -> { throw new RuntimeException("boom"); });
            var result = registry.execute("bad", List.of());
            assertInstanceOf(CommandResult.Error.class, result);
            assertTrue(((CommandResult.Error) result).message().contains("boom"));
        }

        @Test
        void listReturnsAllCommandsSorted() {
            registry.register("zebra", "Z", "/z", args -> new CommandResult.Success(""));
            registry.register("alpha", "A", "/a", args -> new CommandResult.Success(""));
            registry.register("beta", "B", "/b", args -> new CommandResult.Success(""));

            var commands = registry.list();
            assertEquals(3, commands.size());
            assertEquals("alpha", commands.get(0).name());
            assertEquals("beta", commands.get(1).name());
            assertEquals("zebra", commands.get(2).name());
        }

        @Test
        void findReturnsCommandInfo() {
            registry.register("test", "Test cmd", "/test", args -> new CommandResult.Success(""));
            var info = registry.find("test");
            assertNotNull(info);
            assertEquals("test", info.name());
            assertEquals("Test cmd", info.description());
            assertEquals("/test", info.usage());
        }

        @Test
        void findReturnsNullForUnknown() {
            assertNull(registry.find("nonexistent"));
        }
    }

    // ── BuiltinCommands ────────────────────────────────────────────────

    @Nested
    class BuiltinCommandsTests {

        private CommandRegistry registry;
        private BuiltinCommands builtins;

        @BeforeEach
        void setUp() {
            registry = new CommandRegistry();
            builtins = new BuiltinCommands(registry);
            builtins.registerAll();
        }

        @Test
        void helpReturnsSuccessWithCommandList() {
            var result = registry.execute("help", List.of());
            assertInstanceOf(CommandResult.Success.class, result);
            var msg = ((CommandResult.Success) result).message();
            assertTrue(msg.contains("Available commands"));
            assertTrue(msg.contains("help"));
            assertTrue(msg.contains("exit"));
        }

        @Test
        void exitReturnsExit() {
            var result = registry.execute("exit", List.of());
            assertInstanceOf(CommandResult.Exit.class, result);
        }

        @Test
        void quitReturnsExit() {
            var result = registry.execute("quit", List.of());
            assertInstanceOf(CommandResult.Exit.class, result);
        }

        @Test
        void clearReturnsSuccess() {
            var result = registry.execute("clear", List.of());
            assertInstanceOf(CommandResult.Success.class, result);
            assertEquals("Transcript cleared", ((CommandResult.Success) result).message());
        }

        @Test
        void modelWithoutArgsShowsCurrent() {
            var result = registry.execute("model", List.of());
            assertInstanceOf(CommandResult.Success.class, result);
            assertTrue(((CommandResult.Success) result).message().contains("deepseek"));
        }

        @Test
        void modelWithArgsShowsSwitch() {
            var result = registry.execute("model", List.of("gpt-5"));
            assertInstanceOf(CommandResult.Success.class, result);
            assertTrue(((CommandResult.Success) result).message().contains("gpt-5"));
        }

        @Test
        void providerWithoutArgsShowsCurrent() {
            var result = registry.execute("provider", List.of());
            assertInstanceOf(CommandResult.Success.class, result);
            assertTrue(((CommandResult.Success) result).message().contains("deepseek"));
        }

        @Test
        void providerWithArgsShowsSwitch() {
            var result = registry.execute("provider", List.of("openai"));
            assertInstanceOf(CommandResult.Success.class, result);
            assertTrue(((CommandResult.Success) result).message().contains("openai"));
        }

        @Test
        void configWithoutArgsShowsUsage() {
            var result = registry.execute("config", List.of());
            assertInstanceOf(CommandResult.Success.class, result);
            assertTrue(((CommandResult.Success) result).message().contains("config"));
        }

        @Test
        void configWithKey() {
            var result = registry.execute("config", List.of("theme"));
            assertInstanceOf(CommandResult.Success.class, result);
            assertTrue(((CommandResult.Success) result).message().contains("theme"));
        }

        @Test
        void configWithKeyAndValue() {
            var result = registry.execute("config", List.of("theme", "dark"));
            assertInstanceOf(CommandResult.Success.class, result);
            assertTrue(((CommandResult.Success) result).message().contains("dark"));
        }

        @Test
        void threadsCommand() {
            var result = registry.execute("threads", List.of());
            assertInstanceOf(CommandResult.Success.class, result);
        }

        @Test
        void newCommand() {
            var result = registry.execute("new", List.of("test-thread"));
            assertInstanceOf(CommandResult.Success.class, result);
        }

        @Test
        void resumeWithoutArgsReturnsError() {
            var result = registry.execute("resume", List.of());
            assertInstanceOf(CommandResult.Error.class, result);
            assertTrue(((CommandResult.Error) result).message().contains("Usage"));
        }

        @Test
        void resumeWithIdReturnsSuccess() {
            var result = registry.execute("resume", List.of("t42"));
            assertInstanceOf(CommandResult.Success.class, result);
            assertTrue(((CommandResult.Success) result).message().contains("t42"));
        }

        @Test
        void sidebarCommand() {
            var result = registry.execute("sidebar", List.of());
            assertInstanceOf(CommandResult.Success.class, result);
            assertEquals("Sidebar toggled", ((CommandResult.Success) result).message());
        }

        @Test
        void versionCommand() {
            var result = registry.execute("version", List.of());
            assertInstanceOf(CommandResult.Success.class, result);
            assertTrue(((CommandResult.Success) result).message().contains("Jay TUI"));
            assertTrue(((CommandResult.Success) result).message().contains("v0.1.0"));
        }

        @Test
        void allBuiltinCommandsRegistered() {
            var cmds = registry.list();
            assertTrue(cmds.size() >= 11);
            var names = cmds.stream().map(CommandRegistry.CommandInfo::name).toList();
            assertTrue(names.contains("help"));
            assertTrue(names.contains("exit"));
            assertTrue(names.contains("quit"));
            assertTrue(names.contains("clear"));
            assertTrue(names.contains("model"));
            assertTrue(names.contains("provider"));
            assertTrue(names.contains("config"));
            assertTrue(names.contains("threads"));
            assertTrue(names.contains("new"));
            assertTrue(names.contains("resume"));
            assertTrue(names.contains("sidebar"));
            assertTrue(names.contains("version"));
        }
    }
}
