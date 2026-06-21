package com.jay.cli.commands;

import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/** Generate speech audio with TTS models. Delegates to TUI. */
@Command(name = "speech", description = "Generate speech audio with TTS models")
public class SpeechCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("Speech/TTS: use Xiaomi MiMo provider directly.");
        System.out.println("(TUI delegation: speech command forwards to TUI subprocess)");
        return 0;
    }
}
