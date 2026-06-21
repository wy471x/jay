package com.jay.cli.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

/** Run the runtime API / control plane (HTTP/SSE/mobile/stdio). */
@Command(name = "app-server", description = "Run the runtime API / control plane (HTTP/SSE/mobile/stdio)")
public class AppServerCommand implements Callable<Integer> {

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
    public Integer call() {
        if (http) {
            System.out.printf("App server HTTP starting on %s:%d (workers=%d)%n", host, port, workers);
            System.setProperty("jay.server.http", "true");
        } else if (mobile) {
            System.out.printf("App server mobile starting on 0.0.0.0:%d%n", port);
            System.setProperty("jay.server.mobile", "true");
            System.setProperty("jay.server.host", "0.0.0.0");
        } else if (stdio) {
            System.out.println("App server stdio mode starting...");
            System.setProperty("jay.server.stdio", "true");
        } else {
            System.out.printf("App server starting on %s:%d%n", host, port);
        }

        System.setProperty("jay.server.host", host);
        System.setProperty("jay.server.port", String.valueOf(port));
        if (authToken != null) System.setProperty("jay.server.auth-token", authToken);
        if (insecureNoAuth) System.setProperty("jay.server.insecure-no-auth", "true");

        // Delegate to the server module's Spring Boot application
        try {
            Class<?> serverApp = Class.forName("com.jay.server.JayServerApplication");
            org.springframework.boot.SpringApplication.run(serverApp);
            return 0;
        } catch (ClassNotFoundException e) {
            System.err.println("Server module not available on classpath.");
            System.err.println("Add ':server' dependency to cli/build.gradle.kts");
            return 1;
        }
    }
}
