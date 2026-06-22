package com.jay.release;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Release discovery and version comparison.
 *
 * <p>Environment variables (mirroring Rust's):
 * <ul>
 *   <li>{@code CODEWHALE_RELEASE_BASE_URL} — override release asset base URL</li>
 *   <li>{@code DEEPSEEK_TUI_RELEASE_BASE_URL} — legacy alias</li>
 *   <li>{@code DEEPSEEK_RELEASE_BASE_URL} — legacy alias</li>
 *   <li>{@code CODEWHALE_USE_CNB_MIRROR} — enable CNB mirror</li>
 *   <li>{@code DEEPSEEK_TUI_VERSION} — pin update target version</li>
 *   <li>{@code DEEPSEEK_VERSION} — legacy alias</li>
 * </ul>
 */
public final class ReleaseChecker {

    // ── Public constants ────────────────────────────────────────────

    public static final String CHECKSUM_MANIFEST_ASSET = "codewhale-artifacts-sha256.txt";
    public static final String LATEST_RELEASE_URL =
        "https://api.github.com/repos/Hmbown/CodeWhale/releases/latest";
    public static final String RELEASES_URL =
        "https://api.github.com/repos/Hmbown/CodeWhale/releases?per_page=100";
    public static final String CNB_REPO_URL = "https://cnb.cool/codewhale.net/codewhale";
    public static final String RELEASE_BASE_URL_ENV = "CODEWHALE_RELEASE_BASE_URL";
    public static final String LEGACY_RELEASE_BASE_URL_ENV = "DEEPSEEK_TUI_RELEASE_BASE_URL";
    public static final String DEEPSEEK_RELEASE_BASE_URL_ENV = "DEEPSEEK_RELEASE_BASE_URL";
    public static final String CNB_MIRROR_ENV = "CODEWHALE_USE_CNB_MIRROR";
    public static final String UPDATE_VERSION_ENV = "DEEPSEEK_TUI_VERSION";
    public static final String LEGACY_UPDATE_VERSION_ENV = "DEEPSEEK_VERSION";
    public static final String UPDATE_USER_AGENT = "codewhale-updater";

    private static final String CNB_RELEASE_ASSET_BASE =
        "https://cnb.cool/Hmbown/CodeWhale/-/releases";
    private static final Duration RELEASE_METADATA_TIMEOUT = Duration.ofSeconds(5);

    private ReleaseChecker() { }

    // ── Query resolution ───────────────────────────────────────────

    /**
     * Determine the appropriate ReleaseQuery for a given channel.
     * Falls back to the application's version (via system property
     * {@code jay.version}) when no pinned version is set.
     */
    public static ReleaseQuery resolveReleaseQuery(ReleaseChannel channel) {
        String version = updateVersionFromEnv();
        if (version == null) {
            version = System.getProperty("jay.version", "0.1.0");
        }
        String baseUrl = releaseBaseUrlFromEnv(version);
        if (baseUrl != null) {
            return new ReleaseQuery.Mirror(baseUrl, version);
        }
        return switch (channel) {
            case STABLE -> new ReleaseQuery.GitHubLatest(LATEST_RELEASE_URL);
            case BETA -> new ReleaseQuery.GitHubReleaseList(RELEASES_URL);
        };
    }

    /**
     * Read release base URL from environment variables, falling back to
     * the CNB mirror if {@code CODEWHALE_USE_CNB_MIRROR} is set.
     * Returns {@code null} when no override is configured.
     */
    public static String releaseBaseUrlFromEnv(String version) {
        for (String key : new String[]{RELEASE_BASE_URL_ENV, LEGACY_RELEASE_BASE_URL_ENV,
                                       DEEPSEEK_RELEASE_BASE_URL_ENV}) {
            String val = System.getenv(key);
            if (val != null) {
                String trimmed = val.trim();
                if (!trimmed.isEmpty()) return trimmed;
            }
        }
        if (isCnbMirrorEnabled()) {
            return cnbReleaseBaseUrl(version);
        }
        return null;
    }

    /** Read pinned update version from environment variables. */
    public static String updateVersionFromEnv() {
        for (String key : new String[]{UPDATE_VERSION_ENV, LEGACY_UPDATE_VERSION_ENV}) {
            String val = System.getenv(key);
            if (val != null) {
                String trimmed = val.trim();
                if (!trimmed.isEmpty()) {
                    return ReleaseVersion.stripVPrefix(trimmed);
                }
            }
        }
        return null;
    }

    /** Construct CNB mirror asset URL for a given version tag. */
    public static String cnbReleaseBaseUrl(String version) {
        String v = ReleaseVersion.stripVPrefix(version);
        return CNB_RELEASE_ASSET_BASE + "/v" + v;
    }

    static boolean isCnbMirrorEnabled() {
        String val = System.getenv(CNB_MIRROR_ENV);
        return val != null && !val.isBlank() && !"0".equals(val) && !"false".equalsIgnoreCase(val);
    }

    /** Join a mirror base URL with an asset filename. */
    public static String mirrorAssetUrl(String baseUrl, String assetName) {
        String base = baseUrl.replaceAll("/+$", "");
        return base + "/" + assetName;
    }

    // ── HTTP fetching (blocking + async) ────────────────────────────

    private static HttpClient buildClient() {
        return HttpClient.newBuilder()
            .connectTimeout(RELEASE_METADATA_TIMEOUT)
            .build();
    }

    /** Fetch a release JSON payload from a URL (blocking). */
    public static String fetchReleaseJsonBlocking(String url, String description)
            throws IOException {
        var client = buildClient();
        var request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(RELEASE_METADATA_TIMEOUT)
            .header("User-Agent", UPDATE_USER_AGENT)
            .header("Accept", "application/vnd.github+json")
            .GET()
            .build();
        try {
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                throw new IOException(
                    "GitHub release request failed with HTTP " + status + ": " + response.body());
            }
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted fetching " + description + " from " + url, e);
        }
    }

    /** Fetch a release JSON payload from a URL (async). */
    public static CompletableFuture<String> fetchReleaseJsonAsync(String url,
                                                                   String description) {
        var client = buildClient();
        var request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(RELEASE_METADATA_TIMEOUT)
            .header("User-Agent", UPDATE_USER_AGENT)
            .header("Accept", "application/vnd.github+json")
            .GET()
            .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                int status = response.statusCode();
                if (status < 200 || status >= 300) {
                    throw new RuntimeException(
                        "GitHub release request failed with HTTP " + status + ": " + response.body());
                }
                return response.body();
            });
    }

    // ── Tag parsing ─────────────────────────────────────────────────

    /** Extract tag_name from a single-release JSON response. */
    public static String latestTagFromReleaseJson(String body) throws IOException {
        return ReleaseFetcher.parseLatestTag(body);
    }

    /** Extract the first beta tag_name from a release-list JSON response. */
    public static String latestBetaTagFromReleaseListJson(String body) throws IOException {
        return ReleaseFetcher.parseLatestBetaTag(body);
    }

    // ── Latest release tag resolution (blocking + async) ────────────

    /** Resolve the latest release tag for a channel (blocking). */
    public static String latestReleaseTagBlocking(ReleaseChannel channel) throws IOException {
        ReleaseQuery query = resolveReleaseQuery(channel);
        return switch (query) {
            case ReleaseQuery.Mirror m -> "v" + m.version();
            case ReleaseQuery.GitHubLatest g -> {
                String body = fetchReleaseJsonBlocking(g.url(), "latest release");
                yield latestTagFromReleaseJson(body);
            }
            case ReleaseQuery.GitHubReleaseList r -> {
                String body = fetchReleaseJsonBlocking(r.url(), "release list");
                yield latestBetaTagFromReleaseListJson(body);
            }
        };
    }

    /** Resolve the latest release tag for a channel (async). */
    public static CompletableFuture<String> latestReleaseTagAsync(ReleaseChannel channel) {
        ReleaseQuery query = resolveReleaseQuery(channel);
        return switch (query) {
            case ReleaseQuery.Mirror m ->
                CompletableFuture.completedFuture("v" + m.version());
            case ReleaseQuery.GitHubLatest g ->
                fetchReleaseJsonAsync(g.url(), "latest release")
                    .thenApply(body -> {
                        try { return latestTagFromReleaseJson(body); }
                        catch (IOException e) { throw new RuntimeException(e); }
                    });
            case ReleaseQuery.GitHubReleaseList r ->
                fetchReleaseJsonAsync(r.url(), "release list")
                    .thenApply(body -> {
                        try { return latestBetaTagFromReleaseListJson(body); }
                        catch (IOException e) { throw new RuntimeException(e); }
                    });
        };
    }

    // ── Version comparison ─────────────────────────────────────────

    /** Compare a current version against a release tag using semver ordering. */
    public static int compareReleaseVersions(String currentVersion, String latestTag) {
        ReleaseVersion current = ReleaseVersion.parse(currentVersion);
        ReleaseVersion latest = ReleaseVersion.parse(latestTag);
        return current.compareTo(latest);
    }

    /**
     * Determine whether an update is needed.
     *
     * <p>Stable: update when latest is strictly newer.
     * Beta: allows switching from stable to beta on the same release line;
     * otherwise follows normal semver ordering (newer beta wins).
     */
    public static boolean updateIsNeeded(ReleaseChannel channel, String currentVersion,
                                          String latestTag) {
        ReleaseVersion current = ReleaseVersion.parse(currentVersion);
        ReleaseVersion latest = ReleaseVersion.parse(latestTag);

        return switch (channel) {
            case STABLE -> current.compareTo(latest) < 0;

            case BETA -> {
                if (current.equals(latest)) yield false;

                boolean latestIsBeta = latest.isBeta();
                boolean currentIsStable = current.preRelease() == null;
                boolean sameReleaseLine = current.major() == latest.major()
                    && current.minor() == latest.minor()
                    && current.patch() == latest.patch();

                // If current is newer and it's NOT a same-line stable→beta switch, reject
                if (current.compareTo(latest) > 0
                    && !(currentIsStable && sameReleaseLine)) {
                    yield false;
                }
                // Beta channel only cares about beta releases
                yield latestIsBeta;
            }
        };
    }

    /** Return true if the tag name contains "beta" (case-insensitive). */
    public static boolean isBetaTag(String tagName) {
        return tagName != null && tagName.toLowerCase().contains("beta");
    }

    // ── Network fallback hint ──────────────────────────────────────

    /** Human-readable hint for using mirrors when GitHub is blocked. */
    public static String updateNetworkFallbackHint() {
        return "GitHub release downloads may be blocked or slow on this network.\n" +
            " For mainland China, use one of these fallback paths:\n" +
            "   1. Source build from the CNB mirror, installing both shipped binaries:\n" +
            "      cargo install --git " + CNB_REPO_URL + " --tag vX.Y.Z codewhale-cli --locked --force\n" +
            "      cargo install --git " + CNB_REPO_URL + " --tag vX.Y.Z codewhale-tui --locked --force\n" +
            "   2. Use a binary asset mirror:\n" +
            "      " + RELEASE_BASE_URL_ENV + "=https://<mirror>/<release-assets>/ " +
            UPDATE_VERSION_ENV + "=X.Y.Z codewhale update\n" +
            " The mirror directory must contain " + CHECKSUM_MANIFEST_ASSET +
            " and the platform binaries.";
    }
}
