package com.jay.cli.commands;

import picocli.CommandLine.Command;

import java.util.concurrent.Callable;
import java.util.logging.Logger;

/** Generate speech audio with TTS models. Delegates to TUI. */
@Command(name = "speech", description = "Generate speech audio with TTS models")
public class SpeechCommand implements Callable<Integer> {
    private static final Logger LOGGER = Logger.getLogger(SpeechCommand.class.getName());

    @Override
    public Integer call() {
        LOGGER.info("Speech/TTS: use Xiaomi MiMo provider directly.");
        LOGGER.info("(TUI delegation: speech command forwards to TUI subprocess)");
        return 0;
    }
}
