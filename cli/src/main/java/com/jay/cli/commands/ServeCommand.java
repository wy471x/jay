package com.jay.cli.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.concurrent.Callable;

/** Run local server (compatibility alias for app-server). Delegates to TUI. */
@Command(name = "serve", description = "Run local server (compatibility alias for app-server)")
public class ServeCommand implements Callable<Integer> {

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
    public Integer call() {
        System.out.println("Serve: use 'jay app-server' for the canonical runtime API");
        System.out.println("  jay app-server --http    HTTP/SSE runtime API on 127.0.0.1:7878");
        System.out.println("  jay app-server --mobile  Runtime API + phone page (binds 0.0.0.0)");
        System.out.println("  jay app-server --stdio   JSON-RPC over stdio");
        System.out.println("  jay mcp-server           MCP stdio mode");
        return 0;
    }
}
