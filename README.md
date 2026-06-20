<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://img.shields.io/badge/Jay-DeepSeek_Native-6C8EBF?style=for-the-badge&logo=openai&logoColor=white&labelColor=1A1A2E">
    <img src="https://img.shields.io/badge/Jay-DeepSeek_Native-6C8EBF?style=for-the-badge&logo=openai&logoColor=white&labelColor=1A1A2E" alt="Jay">
  </picture>
</p>

<p align="center">
  <strong>DeepSeek-native Coding Agent — Java implementation</strong>
</p>

<p align="center">
  <a href="#features">Features</a> ·
  <a href="#architecture">Architecture</a> ·
  <a href="#getting-started">Getting Started</a> ·
  <a href="#modules">Modules</a> ·
  <a href="#development">Development</a> ·
  <a href="#license">License</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21%2B-ED8B00?style=flat-square&logo=openjdk&logoColor=white" alt="Java 21+">
  <img src="https://img.shields.io/badge/Gradle-9.6-02303A?style=flat-square&logo=gradle&logoColor=white" alt="Gradle 9.6">
  <img src="https://img.shields.io/badge/Spring_Boot-3.4-6DB33F?style=flat-square&logo=springboot&logoColor=white" alt="Spring Boot 3.4">
  <img src="https://img.shields.io/badge/license-MIT-blue.svg?style=flat-square" alt="MIT License">
  <img src="https://img.shields.io/badge/modules-16-6C8EBF?style=flat-square" alt="16 modules">
</p>

---

## Features

- **Multi-provider LLM support** — 80+ built-in models across 20+ providers (DeepSeek, Anthropic, OpenAI, NVIDIA, OpenRouter, and more)
- **Layered execution policy engine** — deny-always-wins, arity-aware command matching, typed ask rules, session approval cache
- **MCP (Model Context Protocol)** — full JSON-RPC 2.0 stdio server with 14 methods for tool/resource lifecycle management
- **Workflow engine (WhaleFlow)** — IR compilation, validation, mock/replay execution, teacher/promotion system
- **Spring Boot HTTP server** — REST API with bearer-token auth, CORS, OpenAI-compatible chat completions proxy
- **Hook event system** — pluggable sinks (stdout, JSONL) for lifecycle observability
- **Virtual threads** — Project Loom (Java 21+) for lightweight concurrency throughout
- **Sealed class + Record types** — exhaustive pattern matching matching Rust's enum semantics

---

## Architecture

```text
┌─────────────────────────────────────────────────────────────┐
│                          CLI (picocli)                       │
├─────────────────────────────────────────────────────────────┤
│                     HTTP Server (Spring Boot)                │
│  /healthz  /thread  /app  /prompt  /tool  /jobs  /mcp/*    │
│  /v1/chat/completions                                       │
├──────────┬──────────┬──────────┬──────────┬─────────────────┤
│   Agent  │   Core   │  Tools   │  State   │   WhaleFlow     │
│  (models)│(runtime) │(dispatch)│(SQLite)  │   (workflow)    │
├──────────┼──────────┼──────────┼──────────┼─────────────────┤
│ ExecPolicy│   MCP   │  Hooks   │  Config  │    Secrets      │
│ (security)│(protocol)│(events) │  (YAML)  │  (KeyStore)     │
├──────────┴──────────┴──────────┴──────────┴─────────────────┤
│                       Protocol (shared types)                │
└─────────────────────────────────────────────────────────────┘
```

Jay is a Java port of the [CodeWhale](https://github.com/deepseek-ai/codewhale) Rust coding agent, maintaining protocol compatibility with the existing Next.js web frontend and Tauri desktop shell. It uses Spring Boot's mature ecosystem to reduce boilerplate while preserving the original's multi-layered architecture.

---

## Getting Started

### Prerequisites

- **Java 21+** — [Adoptium](https://adoptium.net/) or [Oracle JDK](https://jdk.java.net/21/)
- **Gradle 9.6+** — included via wrapper (run `gradle wrapper` first) or install via [Homebrew](https://brew.sh) (`brew install gradle`)

### Build

```bash
# Compile all modules
gradle compileJava

# Run all tests
gradle test

# Build the HTTP server
gradle :server:bootJar
```

### Run the server

```bash
# Start the HTTP server on port 8080
java -jar server/build/libs/server-0.1.0.jar

# With custom port
java -jar server/build/libs/server-0.1.0.jar --server.port=3000
```

### Quick health check

```bash
curl http://localhost:8080/healthz
# {"status":"ok","protocol":"v2","service":"jay-app-server"}
```

---

## Modules

| Module | Description | Key Types |
|--------|-------------|-----------|
| [`protocol`](protocol/) | Shared data types, JSON serialization records | `ThreadRequest`, `AppRequest`, `EventFrame`, `ToolPayload` |
| [`agent`](agent/) | Model registry, provider resolution | `ModelRegistry`, `ModelInfo`, `ModelResolution` |
| [`core`](core/) | Central runtime, thread/job management | `AgentRuntime`, `ThreadManager`, `JobManager` |
| [`tools`](tools/) | Tool registration, dispatch, concurrency control | `ToolRegistry`, `ToolHandler`, `InputExtractors` |
| [`execpolicy`](execpolicy/) | Layered security policy engine | `ExecPolicyEngine`, `BashArityDict`, `Ruleset` |
| [`mcp`](mcp/) | MCP JSON-RPC 2.0 protocol stack | `McpManager`, `StdioMcpServer`, `JsonRpcDispatcher` |
| [`hooks`](hooks/) | Lifecycle event dispatch system | `HookEvent`, `HookDispatcher`, `HookSink` |
| [`state`](state/) | SQLite persistence with Flyway migrations | `StateStore`, `ThreadRepository`, `MessageRepository` |
| [`config`](config/) | Spring `@ConfigurationProperties` | `JayConfig` |
| [`secrets`](secrets/) | KeyStore-based credential management | `SecretsManager` |
| [`server`](server/) | Spring Boot HTTP/WebSocket server | `AppController`, `ChatCompletionsController` |
| [`cli`](cli/) | Picocli command-line interface | `JayCli` |
| [`jayflow`](jayflow/) | Workflow engine (IR, compiler, executor) | `WorkflowConfig`, `WhaleFlowEngine` |
| [`tui`](tui/) | Terminal UI (stub) | `JayTui` |

---

## Development

### Project stats

```text
251 Java source files  ·  13,464 lines of code  ·  16 Gradle submodules
```

### Running tests

```bash
# All modules
gradle test

# Specific module
gradle :execpolicy:test
gradle :mcp:test
gradle :server:test
gradle :hooks:test
```

### Key conventions

- **Java 21** sealed interfaces + records for algebraic data types
- **Jackson** `@JsonTypeInfo` / `@JsonSubTypes` for polymorphic JSON serialization
- **Spring Boot** auto-configuration with `@ConfigurationProperties`
- **Virtual threads** via `Executors.newVirtualThreadPerTaskExecutor()`
- **Constructor injection** — no `@Autowired` on fields
- **JUnit Jupiter 5.11** with standalone MockMvc for controller tests

### Module dependency graph

```text
protocol         (no internal deps)
execpolicy       → protocol, tools
config           → (no internal deps)
secrets          → config
agent            → protocol
state            → protocol
tools            → protocol
mcp              → protocol, tools
hooks            → protocol
jayflow          → core, protocol
tui              → core, protocol
cli              → core, protocol
server           → core, protocol, execpolicy, tools, agent
core             → protocol, agent, tools, execpolicy, config, state
```

---

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 LTS (Virtual Threads) |
| Build | Gradle 9.6 (Kotlin DSL) |
| Framework | Spring Boot 3.4.7 |
| HTTP Server | Tomcat (embedded) |
| JSON | Jackson 2.18 |
| Database | SQLite via JDBC + Flyway |
| CLI | Picocli |
| Testing | JUnit Jupiter 5.11 + MockMvc |
| Security | JDK KeyStore + AES-GCM |

---

## License

[MIT](LICENSE) © 2026 Jay Contributors

---

<p align="center">
  <sub>Built with ❤️ by the Jay team. Protocol-compatible with the <a href="https://github.com/deepseek-ai/codewhale">CodeWhale</a> ecosystem.</sub>
</p>
