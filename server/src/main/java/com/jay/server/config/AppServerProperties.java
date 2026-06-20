package com.jay.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@ConfigurationProperties("jay.server")
public record AppServerProperties(
    String authToken,
    boolean insecureNoAuth,
    List<String> corsOrigins,
    int port
) {
    public AppServerProperties {
        if (corsOrigins == null) corsOrigins = List.of();
    }

    public static List<String> defaultCorsOrigins() {
        return List.of(
            "http://localhost",
            "http://localhost:1420",
            "http://localhost:3000",
            "http://localhost:5173",
            "http://127.0.0.1",
            "http://127.0.0.1:1420",
            "tauri://localhost"
        );
    }
}
