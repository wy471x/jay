package com.jay.cli;

import com.jay.agent.ProviderKind;
import com.jay.cli.commands.*;
import com.jay.cli.converter.ProviderKindConverter;
import com.jay.config.model.CliRuntimeOverrides;
import com.jay.core.RuntimeConfiguration;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

import java.io.File;
import java.util.List;

/**
 * CLI entry point using picocli — replaces Rust's clap-based CLI.
 * Full parity with CodeWhale CLI subcommands per spec 3.13.
 */
@Command(
    name = "jay",
    versionProvider = JayCli.VersionProvider.class,
    description = "Coding Agent — Java Edition",
    mixinStandardHelpOptions = true,
    subcommands = {
        RunCommand.class,
        ExecCommand.class,
        ConfigCommand.class,
        ModelCommand.class,
        ThreadCommand.class,
        AuthCommand.class,
        LoginCommand.class,
        LogoutCommand.class,
        McpServerCommand.class,
        AppServerCommand.class,
        ServeCommand.class,
        CompletionCommand.class,
        DoctorCommand.class,
        MetricsCommand.class,
        UpdateCommand.class,
        SandboxCommand.class,
        ReviewCommand.class,
        InitCommand.class,
        SetupCommand.class,
        SessionsCommand.class,
        ResumeCommand.class,
        ForkCommand.class,
        FleetCommand.class,
        SwebenchCommand.class,
        EvalCommand.class,
        ApplyCommand.class,
        SpeechCommand.class,
        ModelsCommand.class,
        FeaturesCommand.class,
        McpCommand.class,
        RemoteSetupCommand.class,
    }
)
public class JayCli implements Runnable {

    @Spec CommandSpec spec;

    // Static holder so subcommands can access parsed global flags
    private static volatile JayCli instance;

    public JayCli() {
        instance = this; // picocli populates fields after construction, before subcommand execution
    }

    // ── Global flags ─────────────────────────────────────────────────

    @Option(names = {"--config"}, description = "Path to config file")
    File config;

    @Option(names = {"--profile"}, description = "Config profile name")
    String profile;

    @Option(names = {"--provider"}, description = "Advanced provider selector",
            converter = ProviderKindConverter.class)
    ProviderKind provider;

    @Option(names = {"--model"}, description = "Model ID to use")
    String model;

    @Option(names = {"--output-mode"}, description = "Output mode")
    String outputMode;

    @Option(names = {"--verbosity"}, description = "Transcript verbosity (normal, concise)")
    String verbosity;

    @Option(names = {"--log-level"}, description = "Log level")
    String logLevel;

    @Option(names = {"--telemetry"}, description = "Enable/disable telemetry")
    Boolean telemetry;

    @Option(names = {"--approval-policy"}, description = "Tool approval policy")
    String approvalPolicy;

    @Option(names = {"--sandbox-mode"}, description = "Sandbox mode")
    String sandboxMode;

    @Option(names = {"--api-key"}, description = "API key override", hidden = true)
    String apiKey;

    @Option(names = {"--base-url"}, description = "Base URL override")
    String baseUrl;

    @Option(names = {"-C", "--workspace"}, description = "Workspace directory")
    File workspace;

    @Option(names = {"--yolo"}, description = "Auto-approve all tools")
    boolean yolo;

    @Option(names = {"-c", "--continue"}, description = "Continue most recent session")
    boolean continueSession;

    @Option(names = {"-p", "--prompt"}, description = "Initial prompt")
    String promptFlag;

    @Option(names = {"--no-alt-screen"}, description = "Skip alternate screen in TUI")
    boolean noAltScreen;

    @Option(names = {"--mouse-capture"}, fallbackValue = "true",
            negatable = true, description = "Toggle mouse capture in TUI")
    Boolean mouseCapture;

    @Parameters(arity = "0..*", paramLabel = "PROMPT",
            description = "Positional prompt (for default run mode)")
    List<String> prompt;

    // ── Build CLI overrides for subcommands ──────────────────────────

    /** Build CliRuntimeOverrides from the most recently parsed global flags. */
    public static CliRuntimeOverrides buildOverrides() {
        if (instance == null) return new CliRuntimeOverrides();
        var o = new CliRuntimeOverrides();
        if (instance.provider != null) o.provider(instance.provider);
        if (instance.model != null) o.model(instance.model);
        if (instance.apiKey != null) o.apiKey(instance.apiKey);
        if (instance.baseUrl != null) o.baseUrl(instance.baseUrl);
        if (instance.outputMode != null) o.outputMode(instance.outputMode);
        if (instance.logLevel != null) o.logLevel(instance.logLevel);
        if (instance.telemetry != null) o.telemetry(instance.telemetry);
        if (instance.approvalPolicy != null) o.approvalPolicy(instance.approvalPolicy);
        if (instance.sandboxMode != null) o.sandboxMode(instance.sandboxMode);
        if (instance.verbosity != null) o.verbosity(instance.verbosity);
        o.yolo(instance.yolo);
        return o;
    }

    /** Convenience: get the workspace from parsed flags. */
    public static File workspaceFromFlags() {
        if (instance == null) return null;
        return instance.workspace;
    }

    // ── Default behavior ──────────────────────────────────────────────

    @Override
    public void run() {
        boolean hasPrompt = (prompt != null && !prompt.isEmpty()) || promptFlag != null;

        if (!hasPrompt && !continueSession) {
            spec.commandLine().usage(System.out);
            return;
        }

        System.out.println("Starting interactive session...");
        if (continueSession) {
            System.out.println("(continuing most recent session)");
        }
        var promptText = prompt != null && !prompt.isEmpty()
                ? String.join(" ", prompt)
                : promptFlag;
        if (promptText != null && !promptText.isBlank()) {
            System.out.println("Prompt: " + promptText);
        }
        System.out.println("(Use 'jay exec <prompt>' for non-interactive or 'jay app-server --http' for API)");
    }

    // ── Main entry point ──────────────────────────────────────────────

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(RuntimeConfiguration.class);
        CliSpringContext.set(context);

        int exitCode = new picocli.CommandLine(new JayCli())
                .setExecutionStrategy(new picocli.CommandLine.RunLast())
                .setExitCodeExceptionMapper(e -> {
                    System.err.println("Error: " + e.getMessage());
                    return 1;
                })
                .execute(args);

        int springExit = SpringApplication.exit(context, () -> exitCode);
        System.exit(springExit);
    }

    // ── Version provider (embeds build version) ───────────────────────

    static class VersionProvider implements picocli.CommandLine.IVersionProvider {
        @Override
        public String[] getVersion() {
            String version = System.getProperty("jay.version", "0.1.0");
            String buildSha = System.getProperty("jay.build-sha", "");
            String v = buildSha.isEmpty() ? "jay " + version : "jay " + version + " (" + buildSha + ")";
            return new String[]{
                v,
                "Java: " + System.getProperty("java.version"),
                "OS: " + System.getProperty("os.name") + " " + System.getProperty("os.arch"),
                "https://github.com/Hmbown/CodeWhale"
            };
        }
    }
}
