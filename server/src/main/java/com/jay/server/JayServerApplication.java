package com.jay.server;

import com.jay.core.AgentRuntime;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * HTTP server entry point — Spring Boot Web replacing Rust's axum + tower-http.
 * Provides REST API, WebSocket, and SSE endpoints for the web frontend.
 */
@SpringBootApplication
@Import(AgentRuntime.class)
public class JayServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(JayServerApplication.class, args);
    }
}
