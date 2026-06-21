package com.jay.cli.delegate;

import com.jay.config.model.CliRuntimeOverrides;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Facade for delegating CLI subcommands to the TUI subprocess.
 * Bridges resolved CLI options into environment variables consumed by the TUI.
 *
 * <p>Equivalent to Rust's TuiDelegator + env var bridging in lib.rs.
 */
public class TuiDelegator {

    private final Path tuiBinary;

    public TuiDelegator() {
        this(TuiBinaryLocator.locate());
    }

    public TuiDelegator(Path tuiBinary) {
        this.tuiBinary = tuiBinary;
    }

    /**
     * Delegate a subcommand to the TUI binary with full environment bridging.
     *
     * @return the exit code from the TUI process
     */
    public int delegate(String subcommand, List<String> args,
                        CliRuntimeOverrides overrides, String resolvedProvider,
                        String resolvedModel, String resolvedBaseUrl,
                        String resolvedOutputMode, String resolvedLogLevel,
                        Boolean resolvedTelemetry, String resolvedApprovalPolicy,
                        String resolvedSandboxMode, String resolvedVerbosity,
                        String resolvedApiKey, String apiKeySource,
                        File workspace) throws Exception {

        ProcessBuilder pb = new ProcessBuilder();
        List<String> cmd = new ArrayList<>();
        cmd.add(tuiBinary.toString());
        cmd.add(subcommand);
        cmd.addAll(args);
        pb.command(cmd);

        Map<String, String> env = pb.environment();

        // Bridge resolved runtime options as env vars
        if (resolvedProvider != null) {
            env.put("JAY_PROVIDER", resolvedProvider);
            env.put("CODEWHALE_PROVIDER", resolvedProvider);
        }
        if (resolvedModel != null) {
            env.put("JAY_MODEL", resolvedModel);
            env.put("CODEWHALE_MODEL", resolvedModel);
        }
        if (resolvedBaseUrl != null) {
            env.put("JAY_BASE_URL", resolvedBaseUrl);
            env.put("CODEWHALE_BASE_URL", resolvedBaseUrl);
        }
        if (resolvedOutputMode != null) {
            env.put("JAY_OUTPUT_MODE", resolvedOutputMode);
            env.put("CODEWHALE_OUTPUT_MODE", resolvedOutputMode);
        }
        if (resolvedLogLevel != null) {
            env.put("JAY_LOG_LEVEL", resolvedLogLevel);
            env.put("CODEWHALE_LOG_LEVEL", resolvedLogLevel);
        }
        if (resolvedTelemetry != null) {
            env.put("JAY_TELEMETRY", resolvedTelemetry.toString());
            env.put("CODEWHALE_TELEMETRY", resolvedTelemetry.toString());
        }
        if (resolvedApprovalPolicy != null) {
            env.put("JAY_APPROVAL_POLICY", resolvedApprovalPolicy);
            env.put("CODEWHALE_APPROVAL_POLICY", resolvedApprovalPolicy);
        }
        if (resolvedSandboxMode != null) {
            env.put("JAY_SANDBOX_MODE", resolvedSandboxMode);
            env.put("CODEWHALE_SANDBOX_MODE", resolvedSandboxMode);
        }
        if (resolvedVerbosity != null) {
            env.put("JAY_VERBOSITY", resolvedVerbosity);
            env.put("CODEWHALE_VERBOSITY", resolvedVerbosity);
        }
        if (overrides != null && overrides.yolo() != null && overrides.yolo()) {
            env.put("JAY_YOLO", "1");
            env.put("CODEWHALE_YOLO", "1");
        }
        if (resolvedApiKey != null) {
            env.put("JAY_API_KEY", resolvedApiKey);
        }
        if (apiKeySource != null) {
            env.put("JAY_API_KEY_SOURCE", apiKeySource);
            env.put("CODEWHALE_API_KEY_SOURCE", apiKeySource);
        }
        if (workspace != null) {
            env.put("JAY_WORKSPACE", workspace.toString());
            env.put("CODEWHALE_WORKSPACE", workspace.toString());
        }

        Process process = pb.inheritIO().start();
        return process.waitFor();
    }

    /**
     * Simple delegation with minimal metadata — used when full runtime resolution
     * is not needed or the TUI handles its own config loading.
     */
    public int delegateSimple(String subcommand, List<String> args) throws Exception {
        ProcessBuilder pb = new ProcessBuilder();
        List<String> cmd = new ArrayList<>();
        cmd.add(tuiBinary.toString());
        cmd.add(subcommand);
        cmd.addAll(args);
        pb.command(cmd);
        Process process = pb.inheritIO().start();
        return process.waitFor();
    }
}
