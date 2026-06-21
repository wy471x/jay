package com.jay.cli.commands;

import com.jay.agent.ProviderKind;
import org.junit.jupiter.api.*;

import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.*;

class ModelCommandTest {

    @Test
    void listReturnsZero() {
        int code = new CommandLine(new ModelCommand()).execute("list");
        assertEquals(0, code);
    }

    @Test
    void listFilteredByProviderParses() {
        ModelCommand cmd = new ModelCommand();
        new CommandLine(cmd).parseArgs("list", "--provider", "nvidia-nim");
        assertTrue(cmd.list);
        assertEquals(ProviderKind.NVIDIA_NIM, cmd.provider);
    }

    @Test
    void resolveSpecificModelReturnsZero() {
        int code = new CommandLine(new ModelCommand()).execute("claude-sonnet-4-6");
        assertEquals(0, code);
    }

    @Test
    void resolveWithProviderHintReturnsZero() {
        int code = new CommandLine(new ModelCommand()).execute("gpt-5", "--provider", "openai");
        assertEquals(0, code);
    }

    @Test
    void listPrintsModelCount() {
        var oldOut = System.out;
        var bos = new java.io.ByteArrayOutputStream();
        System.setOut(new java.io.PrintStream(bos));
        try {
            new CommandLine(new ModelCommand()).execute("list");
        } finally {
            System.setOut(oldOut);
        }
        assertTrue(bos.toString().contains("model"), "should mention models, got: " +
                bos.toString().substring(0, Math.min(80, bos.size())));
    }
}
