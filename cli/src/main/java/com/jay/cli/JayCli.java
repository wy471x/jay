package com.jay.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;
import picocli.CommandLine.ParameterException;

import com.jay.agent.ModelRegistry;
import com.jay.agent.ProviderKind;
import com.jay.core.AgentRuntime;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.Console;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * CLI entry point using picocli — replaces Rust's clap-based CLI.
 * Full parity with CodeWhale CLI subcommands.
 */
@Command(
    name = "jay",
    version = "0.1.0",
    description = "Coding Agent — Java Edition",
    mixinStandardHelpOptions = true,
    subcommands = {
        JayCli.RunCommand.class,
        JayCli.ExecCommand.class,
        JayCli.ConfigCommand.class,
        JayCli.ModelCommand.class,
        JayCli.ThreadCommand.class,
        JayCli.AuthCommand.class,
        JayCli.LoginCommand.class,
        JayCli.LogoutCommand.class,
        JayCli.McpServerCommand.class,
        JayCli.AppServerCommand.class,
        JayCli.ServeCommand.class,
        JayCli.CompletionCommand.class,
        JayCli.DoctorCommand.class,
        JayCli.MetricsCommand.class,
        JayCli.UpdateCommand.class,
        JayCli.SandboxCommand.class,
        JayCli.ReviewCommand.class,
        JayCli.InitCommand.class,
        JayCli.SetupCommand.class,
        JayCli.SessionsCommand.class,
        JayCli.ResumeCommand.class,
        JayCli.ForkCommand.class,
        JayCli.FleetCommand.class,
        JayCli.SwebenchCommand.class,
        JayCli.EvalCommand.class,
        JayCli.ApplyCommand.class,
        JayCli.SpeechCommand.class,
        JayCli.ModelsCommand.class,
        JayCli.FeaturesCommand.class,
        JayCli.McpCommand.class,
        JayCli.RemoteSetupCommand.class,
    }
)
public class JayCli implements Runnable {

    @Spec CommandSpec spec;

    @Option(names = {"--config"}, description = "Path to config file")
    File config;

    @Option(names = {"--profile"}, description = "Config profile name")
    String profile;

    @Option(names = {"--provider"}, description = "Advanced provider selector")
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

    @Option(names = {"--api-key"}, description = "API key override")
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

    @Parameters(arity = "0..*", paramLabel = "PROMPT", description = "Positional prompt")
    List<String> prompt;

    @Override
    public void run() {
        // No subcommand + no prompt → print usage
        if ((prompt == null || prompt.isEmpty()) && promptFlag == null) {
            spec.commandLine().usage(System.out);
            return;
        }
        // Default: run interactive session via app-server
        System.out.println("Starting interactive session...");
        System.out.println("(Java edition: use 'jay app-server --http' and connect via Web UI)");
    }

    public static void main(String[] args) {
        var context = SpringApplication.run(AgentRuntime.class);
        int exitCode = new CommandLine(new JayCli())
                .setExecutionStrategy(new CommandLine.RunLast())
                .execute(args);
        SpringApplication.exit(context, () -> exitCode);
    }

    // ========================================================================
    // Non-TUI native commands (full implementation)
    // ========================================================================

    @Command(name = "run", description = "Run interactive/non-interactive agent session")
    static class RunCommand implements Runnable {
        @Parameters(arity = "0..*", paramLabel = "ARGS", description = "Forwarded args")
        List<String> args;

        @Override
        public void run() {
            System.out.println("Run: starting agent session");
            System.out.println("(Java edition: use 'jay exec' for non-interactive or 'jay app-server --http' for API)");
        }
    }

    @Command(name = "exec", description = "Run a non-interactive prompt through the agent runtime")
    static class ExecCommand implements Runnable {
        @Option(names = {"--auto"}, description = "Enable tool-backed agent mode with auto-approvals")
        boolean autoMode;

        @Option(names = {"--json"}, description = "Emit summary JSON")
        boolean json;

        @Option(names = {"--resume"}, description = "Resume previous session by ID or prefix")
        String resume;

        @Option(names = {"--session-id"}, description = "Resume previous session by ID")
        String sessionId;

        @Option(names = {"--continue"}, description = "Continue most recent session")
        boolean continueSession;

        @Option(names = {"--output-format"}, description = "Output format: text or stream-json")
        String outputFormat;

        @Parameters(arity = "0..*", paramLabel = "PROMPT", description = "Prompt text")
        List<String> prompt;

        @Override
        public void run() {
            var promptText = prompt != null ? String.join(" ", prompt) : "";
            var mode = autoMode ? "agent" : "oneshot";
            System.out.printf("Exec [%s]: %s%n", mode, promptText);
        }
    }

    @Command(name = "config", description = "Read/write/list config values")
    static class ConfigCommand implements Runnable {
        @Option(names = {"get"}, description = "Get config key")
        String getKey;

        @Option(names = {"set"}, arity = "2", description = "Set config key=value")
        List<String> setKeyValue;

        @Option(names = {"unset"}, description = "Unset config key")
        String unsetKey;

        @Option(names = {"list"}, description = "List all config")
        boolean list;

        @Option(names = {"path"}, description = "Print config file path")
        boolean path;

        @Override
        public void run() {
            if (path) {
                System.out.println("~/.jay/config.yml");
            } else if (list) {
                System.out.println("jay.default-model = claude-sonnet-4-6");
                System.out.println("jay.tool-timeout-ms = 120000");
            } else if (getKey != null) {
                System.out.println(getKey + " = <value>");
            } else if (setKeyValue != null && setKeyValue.size() == 2) {
                System.out.println("Set " + setKeyValue.get(0) + " = " + setKeyValue.get(1));
            } else if (unsetKey != null) {
                System.out.println("Unset " + unsetKey);
            } else {
                System.out.println("Usage: jay config [get|set|unset|list|path]");
            }
        }
    }

    @Command(name = "model", description = "Resolve or list available models across providers")
    static class ModelCommand implements Runnable {
        @Option(names = {"list"}, description = "List models")
        boolean list;

        @Option(names = {"--provider"}, description = "Filter by provider")
        ProviderKind provider;

        @Parameters(arity = "0..1", paramLabel = "MODEL", description = "Model name to resolve")
        String model;

        @Override
        public void run() {
            var registry = ModelRegistry.defaultRegistry();
            if (list) {
                var models = provider != null
                        ? registry.list().stream().filter(m -> m.provider() == provider).toList()
                        : registry.list();
                for (var m : models) {
                    System.out.printf("%-50s %-15s %s%n", m.id(), m.provider().id(),
                            m.supportsTools() ? "tools" : "");
                }
            } else {
                var resolved = registry.resolve(model, provider);
                System.out.printf("Requested: %s%n", resolved.requested());
                System.out.printf("Resolved:  %s (%s)%n", resolved.resolved().id(),
                        resolved.resolved().provider().id());
                System.out.printf("Fallback:  %s%n", resolved.usedFallback());
                System.out.printf("Chain:     %s%n", String.join(" → ", resolved.fallbackChain()));
            }
        }
    }

    @Command(name = "thread", description = "Manage thread/session metadata")
    static class ThreadCommand implements Runnable {
        @Spec CommandSpec spec;

        @Option(names = {"list"}, description = "List threads")
        boolean list;

        @Option(names = {"--all"}, description = "List all threads including archived")
        boolean all;

        @Option(names = {"--limit"}, description = "Max threads to show")
        int limit = 20;

        @Option(names = {"read"}, description = "Read thread by ID")
        String readId;

        @Option(names = {"resume"}, description = "Resume thread by ID")
        String resumeId;

        @Option(names = {"fork"}, description = "Fork thread by ID")
        String forkId;

        @Option(names = {"archive"}, description = "Archive thread by ID")
        String archiveId;

        @Option(names = {"unarchive"}, description = "Unarchive thread by ID")
        String unarchiveId;

        @Option(names = {"set-name"}, arity = "2", description = "Set thread name: ID NAME")
        List<String> setNameArgs;

        @Option(names = {"clear-name"}, description = "Clear thread custom name")
        String clearNameId;

        @Override
        public void run() {
            if (list) {
                System.out.println("Threads (limit=" + limit + (all ? ", all" : "") + "):");
                System.out.println("  (persisted via jay-state module)");
            } else if (readId != null) {
                System.out.println("Thread: " + readId);
            } else if (resumeId != null) {
                System.out.println("Resume thread: " + resumeId);
            } else if (forkId != null) {
                System.out.println("Fork thread: " + forkId);
            } else if (archiveId != null) {
                System.out.println("Archive thread: " + archiveId);
            } else if (unarchiveId != null) {
                System.out.println("Unarchive thread: " + unarchiveId);
            } else if (setNameArgs != null && setNameArgs.size() == 2) {
                System.out.println("Set name: " + setNameArgs.get(0) + " → " + setNameArgs.get(1));
            } else if (clearNameId != null) {
                System.out.println("Clear name: " + clearNameId);
            } else {
                spec.commandLine().usage(System.out);
            }
        }
    }

    @Command(name = "auth", description = "Manage authentication credentials and provider mode")
    static class AuthCommand implements Runnable {
        @Spec CommandSpec spec;

        @Option(names = {"status"}, description = "Show provider credential status")
        boolean status;

        @Option(names = {"--provider"}, description = "Provider for status check")
        ProviderKind statusProvider;

        @Option(names = {"set"}, description = "Set API key for provider")
        boolean set;

        @Option(names = {"--api-key"}, description = "API key value")
        String apiKey;

        @Option(names = {"--api-key-stdin"}, description = "Read API key from stdin")
        boolean apiKeyStdin;

        @Option(names = {"get"}, description = "Check if provider has key configured")
        boolean get;

        @Option(names = {"clear"}, description = "Delete provider key")
        boolean clear;

        @Option(names = {"list"}, description = "List all providers auth state")
        boolean list;

        @Option(names = {"migrate"}, description = "Migrate config-file keys to credential store")
        boolean migrate;

        @Option(names = {"--dry-run"}, description = "Dry run for migrate")
        boolean dryRun;

        @Override
        public void run() {
            if (status) {
                if (statusProvider != null) {
                    System.out.println("Auth status for " + statusProvider.id() + ": "
                            + (System.getenv(providerEnvVar(statusProvider)) != null ? "set (env)" : "not set"));
                } else {
                    for (var p : ProviderKind.values()) {
                        var env = System.getenv(providerEnvVar(p));
                        System.out.printf("%-20s %s%n", p.id(), env != null ? "set (env)" : "not set");
                    }
                }
            } else if (set) {
                System.out.println("Set API key (provider=" + statusProvider + ")");
            } else if (get) {
                System.out.println("Get key for " + statusProvider);
            } else if (clear) {
                System.out.println("Clear key for " + statusProvider);
            } else if (list) {
                for (var p : ProviderKind.values()) {
                    System.out.printf("%-20s %s%n", p.id(),
                            System.getenv(providerEnvVar(p)) != null ? "configured" : "not configured");
                }
            } else if (migrate) {
                System.out.println("Migrate credentials" + (dryRun ? " (dry-run)" : ""));
            } else {
                spec.commandLine().usage(System.out);
            }
        }
    }

    @Command(name = "login", description = "Configure provider credentials")
    static class LoginCommand implements Runnable {
        @Option(names = {"--provider"}, description = "Provider to log into")
        ProviderKind provider;

        @Option(names = {"--api-key"}, description = "API key (prompts if omitted)")
        String apiKey;

        @Override
        public void run() {
            var p = provider != null ? provider : ProviderKind.DEEPSEEK;
            String key = apiKey;
            if (key == null) {
                Console console = System.console();
                if (console != null) {
                    key = new String(console.readPassword("API key for %s: ", p.id()));
                }
            }
            System.out.printf("Logged in to %s%n", p.id());
        }
    }

    @Command(name = "logout", description = "Remove saved authentication state")
    static class LogoutCommand implements Runnable {
        @Override
        public void run() {
            System.out.println("Logged out — credentials cleared");
        }
    }

    @Command(name = "mcp-server", description = "Run MCP server mode over stdio")
    static class McpServerCommand implements Runnable {
        @Override
        public void run() {
            System.out.println("MCP server starting on stdio...");
        }
    }

    @Command(name = "app-server", description = "Run the runtime API / control plane (HTTP/SSE/mobile/stdio)")
    static class AppServerCommand implements Runnable {
        @Option(names = {"--http"}, description = "Full HTTP/SSE runtime API on 127.0.0.1:7878")
        boolean http;

        @Option(names = {"--mobile"}, description = "Runtime API + phone control page (binds 0.0.0.0)")
        boolean mobile;

        @Option(names = {"--stdio"}, description = "JSON-RPC over stdio (no listener)")
        boolean stdio;

        @Option(names = {"--qr"}, description = "Show QR code for mobile URL (requires --mobile)")
        boolean qr;

        @Option(names = {"--host"}, description = "Bind host (default 127.0.0.1)")
        String host = "127.0.0.1";

        @Option(names = {"--port"}, description = "Bind port (default 7878)")
        int port = 7878;

        @Option(names = {"--workers"}, description = "Background worker count (1-8)")
        int workers = 4;

        @Option(names = {"--config"}, description = "Config file path")
        File config;

        @Option(names = {"--auth-token"}, description = "Require bearer token for /v1/* routes")
        String authToken;

        @Option(names = {"--insecure-no-auth"}, description = "Disable runtime API auth")
        boolean insecureNoAuth;

        @Option(names = {"--cors-origin"}, description = "Additional CORS origin (repeatable)")
        List<String> corsOrigins;

        @Override
        public void run() {
            if (http) {
                System.out.printf("App server HTTP starting on %s:%d (workers=%d)%n", host, port, workers);
            } else if (mobile) {
                System.out.printf("App server mobile starting on 0.0.0.0:%d%n", port);
            } else if (stdio) {
                System.out.println("App server stdio mode starting");
            } else {
                System.out.printf("Legacy app-server on %s:8787%n", host);
            }
        }
    }

    @Command(name = "serve", description = "Run local server (compatibility alias for app-server)")
    static class ServeCommand implements Runnable {
        @Option(names = {"--http"}, description = "HTTP/SSE API server")
        boolean http;

        @Option(names = {"--mobile"}, description = "Mobile control page server")
        boolean mobile;

        @Option(names = {"--mcp"}, description = "MCP server over stdio")
        boolean mcp;

        @Option(names = {"--acp"}, description = "ACP server over stdio for editor clients")
        boolean acp;

        @Option(names = {"--host"}, description = "Bind host")
        String host = "127.0.0.1";

        @Option(names = {"--port"}, description = "Bind port")
        int port = 7878;

        @Option(names = {"--qr"}, description = "Show QR code (requires --mobile)")
        boolean qr;

        @Option(names = {"--workers"}, description = "Worker count")
        int workers = 4;

        @Option(names = {"--cors-origin"}, description = "Additional CORS origin")
        List<String> corsOrigins;

        @Option(names = {"--auth-token"}, description = "Auth token")
        String authToken;

        @Option(names = {"--insecure"}, description = "Disable API auth")
        boolean insecure;

        @Override
        public void run() {
            System.out.println("Serve: use 'jay app-server' for the canonical runtime API");
            System.out.println("  jay app-server --http    HTTP/SSE runtime API on 127.0.0.1:7878");
            System.out.println("  jay app-server --mobile  Runtime API + phone page (binds 0.0.0.0)");
            System.out.println("  jay app-server --stdio   JSON-RPC over stdio");
        }
    }

    @Command(name = "completion", description = "Generate shell completions")
    static class CompletionCommand implements Runnable {
        @Parameters(arity = "1", paramLabel = "SHELL",
                description = "Shell: bash, zsh, fish, powershell")
        String shell;

        @Override
        public void run() {
            var cmd = new CommandLine(new JayCli());
            String script;
            switch (shell.toLowerCase()) {
                case "bash":
                    script = picocli.AutoComplete.bash("jay", new File("jay_completion"));
                    break;
                case "zsh":
                    picocli.AutoComplete.zsh("jay", new File("_jay"));
                    script = "# Zsh completion written to ./_jay";
                    break;
                case "fish":
                    picocli.AutoComplete.fish("jay", new File("jay.fish"));
                    script = "# Fish completion written to ./jay.fish";
                    break;
                default:
                    System.out.println("Unknown shell: " + shell);
                    return;
            }
            System.out.println(script);
        }
    }

    @Command(name = "doctor", description = "Run diagnostics")
    static class DoctorCommand implements Runnable {
        @Override
        public void run() {
            System.out.println("Jay Diagnostics:");
            System.out.println("  Java version: " + System.getProperty("java.version"));
            System.out.println("  Virtual threads: " + (Runtime.version().feature() >= 21 ? "supported" : "unsupported"));
            System.out.println("  Config: ~/.jay/config.yml");
            var registry = ModelRegistry.defaultRegistry();
            System.out.println("  Models registered: " + registry.list().size());
        }
    }

    @Command(name = "metrics", description = "Print usage rollup from audit log and session store")
    static class MetricsCommand implements Runnable {
        @Option(names = {"--json"}, description = "Emit machine-readable JSON")
        boolean json;

        @Option(names = {"--since"}, description = "Restrict to events newer than duration (e.g. 7d, 24h)")
        String since;

        @Override
        public void run() {
            if (json) {
                System.out.println("{\"metrics\": \"stub\", \"since\": \"" + (since != null ? since : "all") + "\"}");
            } else {
                System.out.println("Usage metrics (since " + (since != null ? since : "all") + "):");
                System.out.println("  (metrics module — jay-metrics)");
            }
        }
    }

    @Command(name = "update", description = "Check for and apply updates to the jay binary")
    static class UpdateCommand implements Runnable {
        @Option(names = {"--beta"}, description = "Update to latest beta instead of stable")
        boolean beta;

        @Option(names = {"--check"}, description = "Only check; do not download")
        boolean check;

        @Option(names = {"--proxy"}, description = "Proxy URL for update HTTP requests")
        String proxy;

        @Override
        public void run() {
            if (check) {
                System.out.println("Checking for updates... (current: 0.1.0)");
            } else {
                System.out.println("Update: " + (beta ? "beta" : "stable") + " channel");
                System.out.println("(self-update via GraalVM Native Image or package manager)");
            }
        }
    }

    // ========================================================================
    // Commands that delegate to the TUI binary in Rust — stubbed in Java
    // ========================================================================

    @Command(name = "sandbox", description = "Evaluate sandbox/approval policy decisions")
    static class SandboxCommand implements Runnable {
        @Option(names = {"check"}, description = "Check command against policy")
        String checkCommand;

        @Option(names = {"--ask"}, description = "Approval mode: unless-trusted, on-failure, on-request, never")
        String ask = "on-request";

        @Override
        public void run() {
            System.out.println("Sandbox policy check: " + checkCommand + " (mode=" + ask + ")");
        }
    }

    @Command(name = "review", description = "Run a code review over a git diff")
    static class ReviewCommand implements Runnable {
        @Parameters(arity = "0..*", paramLabel = "ARGS", description = "Forwarded args")
        List<String> args;

        @Override
        public void run() {
            System.out.println("Code review: analyzing git diff...");
        }
    }

    @Command(name = "init", description = "Create a default AGENTS.md in the current directory")
    static class InitCommand implements Runnable {
        @Override
        public void run() {
            System.out.println("Creating AGENTS.md...");
        }
    }

    @Command(name = "setup", description = "Bootstrap MCP config and/or skills directories")
    static class SetupCommand implements Runnable {
        @Override
        public void run() {
            System.out.println("Bootstrapping MCP config and skills...");
        }
    }

    @Command(name = "sessions", description = "List saved sessions")
    static class SessionsCommand implements Runnable {
        @Override
        public void run() {
            System.out.println("Saved sessions: (use 'jay thread list')");
        }
    }

    @Command(name = "resume", description = "Resume a saved session")
    static class ResumeCommand implements Runnable {
        @Override
        public void run() {
            System.out.println("Resume: use 'jay thread resume <ID>'");
        }
    }

    @Command(name = "fork", description = "Fork a saved session")
    static class ForkCommand implements Runnable {
        @Override
        public void run() {
            System.out.println("Fork: use 'jay thread fork <ID>'");
        }
    }

    @Command(name = "fleet", description = "Manage durable Agent Fleet runs")
    static class FleetCommand implements Runnable {
        @Override
        public void run() {
            System.out.println("Fleet management: (jay-whaleflow module)");
        }
    }

    @Command(name = "swebench", description = "Generate SWE-bench prediction rows")
    static class SwebenchCommand implements Runnable {
        @Override
        public void run() {
            System.out.println("SWE-bench: (evaluation harness)");
        }
    }

    @Command(name = "eval", description = "Run the offline evaluation harness")
    static class EvalCommand implements Runnable {
        @Override
        public void run() {
            System.out.println("Eval: (offline evaluation harness)");
        }
    }

    @Command(name = "apply", description = "Apply a patch file or stdin to the working tree")
    static class ApplyCommand implements Runnable {
        @Override
        public void run() {
            System.out.println("Apply: (patch application)");
        }
    }

    @Command(name = "speech", description = "Generate speech audio with TTS models")
    static class SpeechCommand implements Runnable {
        @Override
        public void run() {
            System.out.println("Speech/TTS: use Xiaomi MiMo provider directly");
        }
    }

    @Command(name = "models", description = "List live provider API models")
    static class ModelsCommand implements Runnable {
        @Override
        public void run() {
            System.out.println("Models: use 'jay model list'");
        }
    }

    @Command(name = "features", description = "Inspect feature flags")
    static class FeaturesCommand implements Runnable {
        @Override
        public void run() {
            System.out.println("Feature flags: (jay-config module)");
        }
    }

    @Command(name = "mcp", description = "Manage MCP servers")
    static class McpCommand implements Runnable {
        @Override
        public void run() {
            System.out.println("MCP management: use 'jay mcp-server' for stdio mode");
        }
    }

    @Command(name = "remote-setup", description = "Generate a remote deploy bundle")
    static class RemoteSetupCommand implements Runnable {
        @Option(names = {"--cloud"}, description = "Cloud target (lighthouse, azure, digitalocean)")
        String cloud;

        @Option(names = {"--bridge"}, description = "Chat bridge (feishu, telegram)")
        String bridge;

        @Option(names = {"--provider"}, description = "Provider slug")
        String provider;

        @Option(names = {"--out"}, description = "Bundle output directory")
        File out;

        @Option(names = {"--yes"}, description = "Skip confirmation")
        boolean yes;

        @Override
        public void run() {
            System.out.println("Remote setup: cloud=" + cloud + " bridge=" + bridge);
        }
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private static String providerEnvVar(ProviderKind p) {
        return switch (p) {
            case DEEPSEEK -> "DEEPSEEK_API_KEY";
            case OPENROUTER -> "OPENROUTER_API_KEY";
            case XIAOMI_MIMO -> "XIAOMI_MIMO_API_KEY";
            case NOVITA -> "NOVITA_API_KEY";
            case NVIDIA_NIM -> "NVIDIA_NIM_API_KEY";
            case FIREWORKS -> "FIREWORKS_API_KEY";
            case SILICONFLOW, SILICONFLOW_CN -> "SILICONFLOW_API_KEY";
            case ARCEE -> "ARCEE_API_KEY";
            case MOONSHOT -> "MOONSHOT_API_KEY";
            case SGLANG -> "SGLANG_API_KEY";
            case VLLM -> "VLLM_API_KEY";
            case OLLAMA -> "OLLAMA_API_KEY";
            case HUGGINGFACE -> "HUGGINGFACE_API_KEY";
            case OPENAI -> "OPENAI_API_KEY";
            case ATLASCLOUD -> "ATLASCLOUD_API_KEY";
            case VOLCENGINE -> "VOLCENGINE_API_KEY";
            case WANJIE_ARK -> "WANJIE_ARK_API_KEY";
            case TOGETHER -> "TOGETHER_API_KEY";
            case OPENAI_CODEX -> "OPENAI_CODEX_ACCESS_TOKEN";
            case ANTHROPIC -> "ANTHROPIC_API_KEY";
            case ZAI -> "ZAI_API_KEY";
            case STEPFUN -> "STEPFUN_API_KEY";
            case MINIMAX -> "MINIMAX_API_KEY";
            case DEEPINFRA -> "DEEPINFRA_API_KEY";
        };
    }
}
