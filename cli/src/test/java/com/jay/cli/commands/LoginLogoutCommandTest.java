package com.jay.cli.commands;

import org.junit.jupiter.api.*;

import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.*;

class LoginLogoutCommandTest {

    @Nested
    class Login {

        @Test
        void providerParses() {
            LoginCommand cmd = new LoginCommand();
            new CommandLine(cmd).parseArgs("--provider", "deepseek");
            assertNotNull(cmd.provider);
        }

        @Test
        void defaultsToNullProvider() {
            LoginCommand cmd = new LoginCommand();
            new CommandLine(cmd).parseArgs("--api-key", "sk-test");
            assertNull(cmd.provider);
            assertEquals("sk-test", cmd.apiKey);
        }

        @Test
        void failsWhenNoApiKeyAndNoConsole() {
            int code = new CommandLine(new LoginCommand())
                    .execute("--provider", "deepseek");
            assertEquals(1, code);
        }

        @Test
        void succeedsWithApiKey() {
            int code = new CommandLine(new LoginCommand())
                    .execute("--provider", "deepseek", "--api-key", "sk-test-12345");
            assertEquals(0, code);
        }
    }

    @Nested
    class Logout {

        @Test
        void succeeds() {
            int code = new CommandLine(new LogoutCommand()).execute();
            assertEquals(0, code);
        }
    }
}
