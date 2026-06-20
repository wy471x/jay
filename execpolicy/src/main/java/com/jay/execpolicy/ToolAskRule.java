package com.jay.execpolicy;

public record ToolAskRule(String tool, String command, String path) {

    public ToolAskRule {
        if (tool == null) throw new IllegalArgumentException("tool must not be null");
    }

    public ToolAskRule(String tool) {
        this(tool, null, null);
    }

    /** Convenience: typed ask rule for exec_shell with a specific command prefix. */
    public static ToolAskRule execShell(String command) {
        return new ToolAskRule("exec_shell", command, null);
    }

    /** Convenience: typed ask rule for a file-tool with a specific path. */
    public static ToolAskRule filePath(String tool, String path) {
        return new ToolAskRule(tool, null, path);
    }

    /** Human-readable label for this rule. */
    public String label() {
        var sb = new StringBuilder("tool=").append(tool);
        if (command != null) sb.append(" command=").append(command);
        if (path != null) sb.append(" path=").append(path);
        return sb.toString();
    }

    /**
     * Specificity score for tie-breaking.
     * Base = tool length; + 1000 + length if command is present; + 1000 + length if path is present.
     */
    public int specificity() {
        int score = tool.length();
        if (command != null) score += 1000 + command.length();
        if (path != null) score += 1000 + path.length();
        return score;
    }
}
