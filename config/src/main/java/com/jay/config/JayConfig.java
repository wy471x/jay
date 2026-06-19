package com.jay.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Central application configuration using Spring Boot's
 * @ConfigurationProperties — replaces Rust's TOML-based config system.
 * Annotated properties are auto-bound from application.yml.
 */
@Configuration
@ConfigurationProperties(prefix = "jay")
public class JayConfig {

    private String defaultModel = "claude-sonnet-4-6";
    private int maxConversationTurns = 100;
    private long toolTimeoutMs = 120_000;
    private boolean toolApprovalRequired = false;
    private List<String> allowedTools = List.of();
    private Database database = new Database();
    private Security security = new Security();
    private Mcp mcp = new Mcp();
    private Logging logging = new Logging();

    public String defaultModel() { return defaultModel; }
    public void setDefaultModel(String v) { this.defaultModel = v; }

    public int maxConversationTurns() { return maxConversationTurns; }
    public void setMaxConversationTurns(int v) { this.maxConversationTurns = v; }

    public long toolTimeoutMs() { return toolTimeoutMs; }
    public void setToolTimeoutMs(long v) { this.toolTimeoutMs = v; }

    public boolean toolApprovalRequired() { return toolApprovalRequired; }
    public void setToolApprovalRequired(boolean v) { this.toolApprovalRequired = v; }

    public List<String> allowedTools() { return allowedTools; }
    public void setAllowedTools(List<String> v) { this.allowedTools = v; }

    public Database database() { return database; }
    public void setDatabase(Database v) { this.database = v; }

    public Security security() { return security; }
    public void setSecurity(Security v) { this.security = v; }

    public Mcp mcp() { return mcp; }
    public void setMcp(Mcp v) { this.mcp = v; }

    public Logging logging() { return logging; }
    public void setLogging(Logging v) { this.logging = v; }

    public static class Database {
        private String url = "jdbc:sqlite:jay.db";
        private String driver = "org.sqlite.JDBC";
        public String url() { return url; }
        public void setUrl(String v) { this.url = v; }
        public String driver() { return driver; }
        public void setDriver(String v) { this.driver = v; }
    }

    public static class Security {
        private String keystorePath = "jay.keystore";
        private String keystoreType = "PKCS12";
        public String keystorePath() { return keystorePath; }
        public void setKeystorePath(String v) { this.keystorePath = v; }
        public String keystoreType() { return keystoreType; }
        public void setKeystoreType(String v) { this.keystoreType = v; }
    }

    public static class Mcp {
        private int serverPort = 0;
        private long connectTimeoutMs = 30_000;
        public int serverPort() { return serverPort; }
        public void setServerPort(int v) { this.serverPort = v; }
        public long connectTimeoutMs() { return connectTimeoutMs; }
        public void setConnectTimeoutMs(long v) { this.connectTimeoutMs = v; }
    }

    public static class Logging {
        private String level = "INFO";
        private boolean jsonFormat = false;
        public String level() { return level; }
        public void setLevel(String v) { this.level = v; }
        public boolean jsonFormat() { return jsonFormat; }
        public void setJsonFormat(boolean v) { this.jsonFormat = v; }
    }
}
