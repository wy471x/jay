package com.jay.tui.core;

/**
 * Classifies engine errors for retry/recover decisions.
 * Mirrors Rust {@code error_taxonomy.rs}.
 */
public class ErrorTaxonomy {

    public enum Category {
        NETWORK,
        RATE_LIMIT,
        TIMEOUT,
        AUTH,
        INVALID_INPUT,
        PARSE,
        INTERNAL
    }

    public record ErrorEnvelope(Category category, String message, boolean recoverable) {}

    // ── Classification ──────────────────────────────────────────────

    /** Classify an error message into a category. */
    public static ErrorEnvelope classify(String errorMessage) {
        if (errorMessage == null) {
            return new ErrorEnvelope(Category.INTERNAL, "Unknown error", false);
        }

        String lower = errorMessage.toLowerCase();

        // Network & timeout errors — transient, recoverable
        if (lower.contains("connection") || lower.contains("connect")
                || lower.contains("refused") || lower.contains("reset")
                || lower.contains("tls") || lower.contains("handshake")
                || lower.contains("timed out") || lower.contains("timeout")) {
            if (lower.contains("timeout") || lower.contains("timed out")) {
                return new ErrorEnvelope(Category.TIMEOUT, errorMessage, true);
            }
            return new ErrorEnvelope(Category.NETWORK, errorMessage, true);
        }

        // Rate limit — recoverable after wait
        if (lower.contains("rate") && (lower.contains("limit") || lower.contains("exceeded"))
                || lower.contains("429") || lower.contains("too many requests")) {
            return new ErrorEnvelope(Category.RATE_LIMIT, errorMessage, true);
        }

        // Auth errors — NOT recoverable
        if (lower.contains("unauthorized") || lower.contains("forbidden")
                || lower.contains("401") || lower.contains("403")
                || lower.contains("invalid api key") || lower.contains("authentication")) {
            return new ErrorEnvelope(Category.AUTH, errorMessage, false);
        }

        // Parse errors — NOT recoverable (bad model output)
        if (lower.contains("parse") || lower.contains("json")
                || lower.contains("malformed")) {
            return new ErrorEnvelope(Category.PARSE, errorMessage, false);
        }

        // Invalid input — NOT recoverable
        if (lower.contains("invalid") || lower.contains("bad request")
                || lower.contains("400")) {
            return new ErrorEnvelope(Category.INVALID_INPUT, errorMessage, false);
        }

        // Default: internal, not recoverable
        return new ErrorEnvelope(Category.INTERNAL, errorMessage, false);
    }

    /** Whether an error category should trigger a retry. */
    public static boolean shouldRetry(Category category) {
        return switch (category) {
            case NETWORK, RATE_LIMIT, TIMEOUT -> true;
            case AUTH, INVALID_INPUT, PARSE, INTERNAL -> false;
        };
    }
}
