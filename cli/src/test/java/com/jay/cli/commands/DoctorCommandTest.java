package com.jay.cli.commands;

import org.junit.jupiter.api.*;

import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.*;

class DoctorCommandTest {

    @Test
    void runsSuccessfully() {
        int code = new CommandLine(new DoctorCommand()).execute();
        assertEquals(0, code);
    }

    @Test
    void printsExpectedSections() {
        // DoctorCommand uses System.out directly; capture via redirect
        var oldOut = System.out;
        var bos = new java.io.ByteArrayOutputStream();
        System.setOut(new java.io.PrintStream(bos));
        try {
            new CommandLine(new DoctorCommand()).execute();
        } finally {
            System.setOut(oldOut);
        }
        String out = bos.toString();
        assertTrue(out.contains("Java version") || out.contains("Runtime"),
                "should contain diagnostics header");
    }
}
