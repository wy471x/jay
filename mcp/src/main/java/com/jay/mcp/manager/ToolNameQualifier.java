package com.jay.mcp.manager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class ToolNameQualifier {

    private static final int MAX_LEN = 64;
    private static final String PREFIX = "mcp__";

    private ToolNameQualifier() { }

    public static String qualify(String serverName, String toolName) {
        String srv = sanitize(serverName);
        String tool = sanitize(toolName);
        String name = PREFIX + srv + "__" + tool;
        if (name.length() <= MAX_LEN) return name;

        String hash = sha256Hex(name).substring(0, 12);
        String suffix = "_" + hash;
        int componentBudget = MAX_LEN - PREFIX.length() - "__".length() - suffix.length();

        int serverLen = Math.min(srv.length(), componentBudget / 2);
        int toolLen = Math.min(tool.length(), componentBudget - serverLen);
        int remaining = componentBudget - serverLen - toolLen;
        if (remaining > 0) {
            int serverExtra = Math.min(srv.length() - serverLen, remaining);
            serverLen += serverExtra;
            toolLen += Math.min(tool.length() - toolLen, remaining - serverExtra);
        }
        return PREFIX + srv.substring(0, serverLen) + "__" + tool.substring(0, toolLen) + suffix;
    }

    public static String[] parse(String qualified) {
        if (qualified == null || !qualified.startsWith(PREFIX)) return null;
        String rest = qualified.substring(PREFIX.length());
        int sep = rest.indexOf("__");
        if (sep <= 0) return null;
        String server = rest.substring(0, sep);
        String tool = rest.substring(sep + 2);
        if (server.isEmpty() || tool.isEmpty()) return null;
        // strip trailing _hash suffix if present
        int hashIdx = tool.lastIndexOf('_');
        if (hashIdx > 0 && hashIdx < tool.length() - 1) {
            String possibleHash = tool.substring(hashIdx + 1);
            if (possibleHash.length() >= 8 && possibleHash.matches("[0-9a-f]+")) {
                tool = tool.substring(0, hashIdx);
            }
        }
        return new String[] { server, tool };
    }

    public static String sanitize(String input) {
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (ch >= 'a' && ch <= 'z' || ch >= '0' && ch <= '9' || ch == '_') {
                sb.append(ch);
            } else if (ch >= 'A' && ch <= 'Z') {
                sb.append((char) (ch + 32));
            } else {
                sb.append('_');
            }
        }
        return sb.toString();
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
