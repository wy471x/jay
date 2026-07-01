package com.jay.tui.core.seam;

/**
 * Configuration for the Flash seam manager (append-only layered context).
 *
 * <h3>Soft seam levels</h3>
 * <table>
 *   <tr><th>Level</th><th>Active input trigger</th><th>Covers messages</th><th>Density</th></tr>
 *   <tr><td>L1</td><td>192K tokens</td><td>0–128K</td><td>~3,200 tokens</td></tr>
 *   <tr><td>L2</td><td>384K tokens</td><td>0–320K</td><td>~2,400 tokens</td></tr>
 *   <tr><td>L3</td><td>576K tokens</td><td>0–512K</td><td>~1,600 tokens</td></tr>
 * </table>
 *
 * <p>Thresholds derived from V4 paper Figure 9 (MMR): 128K→256K is the real cliff.
 * L1 triggers at 192K, before the cliff.
 */
public class SeamConfig {

    public static final String DEFAULT_SEAM_MODEL = "deepseek-v4-flash";
    public static final int DEFAULT_L1_THRESHOLD = 192_000;
    public static final int DEFAULT_L2_THRESHOLD = 384_000;
    public static final int DEFAULT_L3_THRESHOLD = 576_000;
    public static final int VERBATIM_WINDOW_TURNS = 16;

    // Token caps per level
    static final int L1_MAX_TOKENS = 3_200;
    static final int L2_MAX_TOKENS = 2_400;
    static final int L3_MAX_TOKENS = 1_600;

    private final boolean enabled;
    private final int verbatimWindowTurns;
    private final int l1Threshold;
    private final int l2Threshold;
    private final int l3Threshold;
    private final String seamModel;

    public SeamConfig(boolean enabled, int verbatimWindowTurns,
                       int l1Threshold, int l2Threshold, int l3Threshold,
                       String seamModel) {
        this.enabled = enabled;
        this.verbatimWindowTurns = verbatimWindowTurns;
        this.l1Threshold = l1Threshold;
        this.l2Threshold = l2Threshold;
        this.l3Threshold = l3Threshold;
        this.seamModel = seamModel;
    }

    public static SeamConfig defaultConfig() {
        return new SeamConfig(true, VERBATIM_WINDOW_TURNS,
                DEFAULT_L1_THRESHOLD, DEFAULT_L2_THRESHOLD,
                DEFAULT_L3_THRESHOLD, DEFAULT_SEAM_MODEL);
    }

    public static SeamConfig disabled() {
        return new SeamConfig(false, VERBATIM_WINDOW_TURNS,
                DEFAULT_L1_THRESHOLD, DEFAULT_L2_THRESHOLD,
                DEFAULT_L3_THRESHOLD, DEFAULT_SEAM_MODEL);
    }

    public boolean enabled() { return enabled; }
    public int verbatimWindowTurns() { return verbatimWindowTurns; }
    public int l1Threshold() { return l1Threshold; }
    public int l2Threshold() { return l2Threshold; }
    public int l3Threshold() { return l3Threshold; }
    public String seamModel() { return seamModel; }

    /** Get the max_tokens for a given seam level. */
    public static int maxTokensForLevel(int level) {
        return switch (level) {
            case 1 -> L1_MAX_TOKENS;
            case 2 -> L2_MAX_TOKENS;
            case 3 -> L3_MAX_TOKENS;
            default -> L3_MAX_TOKENS;
        };
    }

    /** Get the word limit for the summary prompt at a given level. */
    public static int wordLimitForLevel(int level) {
        return switch (level) {
            case 1 -> 800;
            case 2 -> 600;
            case 3 -> 400;
            default -> 400;
        };
    }

    /** Density label for the archived_context block at a given level. */
    public static String densityLabel(int level) {
        return switch (level) {
            case 1 -> "~3,200 tokens";
            case 2 -> "~2,400 tokens";
            case 3 -> "~1,600 tokens";
            default -> "unknown";
        };
    }
}
