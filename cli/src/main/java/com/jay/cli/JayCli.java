package com.jay.cli;

import com.jay.agent.ProviderKind;
import com.jay.cli.commands.AppServerCommand;
import com.jay.cli.commands.ApplyCommand;
import com.jay.cli.commands.AuthCommand;
import com.jay.cli.commands.CompletionCommand;
import com.jay.cli.commands.ConfigCommand;
import com.jay.cli.commands.DoctorCommand;
import com.jay.cli.commands.EvalCommand;
import com.jay.cli.commands.ExecCommand;
import com.jay.cli.commands.FeaturesCommand;
import com.jay.cli.commands.FleetCommand;
import com.jay.cli.commands.ForkCommand;
import com.jay.cli.commands.InitCommand;
import com.jay.cli.commands.LoginCommand;
import com.jay.cli.commands.LogoutCommand;
import com.jay.cli.commands.McpCommand;
import com.jay.cli.commands.McpServerCommand;
import com.jay.cli.commands.MetricsCommand;
import com.jay.cli.commands.ModelCommand;
import com.jay.cli.commands.ModelsCommand;
import com.jay.cli.commands.RemoteSetupCommand;
import com.jay.cli.commands.ResumeCommand;
import com.jay.cli.commands.ReviewCommand;
import com.jay.cli.commands.RunCommand;
import com.jay.cli.commands.SandboxCommand;
import com.jay.cli.commands.ServeCommand;
import com.jay.cli.commands.SessionsCommand;
import com.jay.cli.commands.SetupCommand;
import com.jay.cli.commands.SpeechCommand;
import com.jay.cli.commands.SwebenchCommand;
import com.jay.cli.commands.ThreadCommand;
import com.jay.cli.commands.UpdateCommand;
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
import java.util.logging.Logger;

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
    private static final Logger LOGGER = Logger.getLogger(JayCli.class.getName());

    @Spec CommandSpec spec;

    // Static holder so subcommands can access parsed global flags
    private static volatile JayCli INSTANCE;

    public JayCli() {
        INSTANCE = this; // picocli populates fields after construction, before subcommand execution
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
        if (INSTANCE == null) return new CliRuntimeOverrides();
        var o = new CliRuntimeOverrides();
        if (INSTANCE.provider != null) o.provider(INSTANCE.provider);
        if (INSTANCE.model != null) o.model(INSTANCE.model);
        if (INSTANCE.apiKey != null) o.apiKey(INSTANCE.apiKey);
        if (INSTANCE.baseUrl != null) o.baseUrl(INSTANCE.baseUrl);
        if (INSTANCE.outputMode != null) o.outputMode(INSTANCE.outputMode);
        if (INSTANCE.logLevel != null) o.logLevel(INSTANCE.logLevel);
        if (INSTANCE.telemetry != null) o.telemetry(INSTANCE.telemetry);
        if (INSTANCE.approvalPolicy != null) o.approvalPolicy(INSTANCE.approvalPolicy);
        if (INSTANCE.sandboxMode != null) o.sandboxMode(INSTANCE.sandboxMode);
        if (INSTANCE.verbosity != null) o.verbosity(INSTANCE.verbosity);
        o.yolo(INSTANCE.yolo);
        return o;
    }

    /** Convenience: get the workspace from parsed flags. */
    public static File workspaceFromFlags() {
        if (INSTANCE == null) return null;
        return INSTANCE.workspace;
    }

    // ── Default behavior ──────────────────────────────────────────────

    @Override
    public void run() {
        boolean hasPrompt = prompt != null && !prompt.isEmpty() || promptFlag != null;

        if (!hasPrompt && !continueSession) {
            spec.commandLine().usage(System.out);
            return;
        }

        LOGGER.info("Starting interactive session...");
        if (continueSession) {
            LOGGER.info("(continuing most recent session)");
        }
        var promptText = prompt != null && !prompt.isEmpty()
                ? String.join(" ", prompt)
                : promptFlag;
        if (promptText != null && !promptText.isBlank()) {
            LOGGER.info("Prompt: " + promptText);
        }
        LOGGER.info("(Use 'jay exec <prompt>' for non-interactive or 'jay app-server --http' for API)");
    }

    // ── Main entry point ──────────────────────────────────────────────

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(RuntimeConfiguration.class);
        CliSpringContext.set(context);

        int exitCode = new picocli.CommandLine(new JayCli())
                .setExecutionStrategy(new picocli.CommandLine.RunLast())
                .setExitCodeExceptionMapper(e -> {
                    LOGGER.severe("Error: " + e.getMessage());
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
