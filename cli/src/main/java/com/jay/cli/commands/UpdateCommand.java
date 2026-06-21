package com.jay.cli.commands;

import com.jay.cli.update.UpdateRunner;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

/** Check for and apply updates to the jay binary. */
@Command(name = "update", description = "Check for and apply updates to the jay binary")
public class UpdateCommand implements Callable<Integer> {

    @Option(names = {"--beta"}, description = "Update to latest beta instead of stable")
    boolean beta;

    @Option(names = {"--check"}, description = "Only check; do not download")
    boolean check;

    @Option(names = {"--proxy"}, description = "Proxy URL for update HTTP requests")
    String proxy;

    @Override
    public Integer call() {
        UpdateRunner runner = new UpdateRunner(beta, check, proxy);
        return runner.run();
    }
}
