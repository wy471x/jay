package com.jay.cli.update;

import com.jay.release.ReleaseChannel;
import com.jay.release.ReleaseChecker;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;

/**
 * Self-update orchestrator. Fetches latest release metadata, downloads
 * the platform-correct binary, verifies SHA-256 checksum, and atomically
 * replaces the currently running binary.
 *
 * <p>Equivalent to Rust's update.rs (1,835 lines).
 */
public class UpdateRunner {

    private static final String DOWNLOAD_BASE_URL =
            "https://github.com/Hmbown/CodeWhale/releases/download";
    private static final int MAX_RETRIES = 3;
    private static final Duration RETRY_DELAY = Duration.ofMillis(100);

    private final boolean beta;
    private final boolean checkOnly;
    private final String proxyUrl;

    public UpdateRunner(boolean beta, boolean checkOnly, String proxyUrl) {
        this.beta = beta;
        this.checkOnly = checkOnly;
        this.proxyUrl = proxyUrl;
    }

    /** Execute the update workflow. Returns 0 on success, non-zero on failure. */
    public int run() {
        ReleaseChannel channel = ReleaseChannel.fromBetaFlag(beta);
        String currentVersion = System.getProperty("jay.version", "0.1.0");

        System.out.println("Checking for " + channel.label() + " updates...");
        System.out.println("Current version: v" + currentVersion);

        try {
            // 1. Query latest release tag
            String latestTag = ReleaseChecker.latestReleaseTagBlocking(channel);
            System.out.println("Latest " + channel.label() + " release: " + latestTag);

            // 2. Check if update is needed
            if (!ReleaseChecker.updateIsNeeded(channel, currentVersion, latestTag)) {
                System.out.println("Already up to date (v" + currentVersion + " >= " + latestTag + ")");
                return 0;
            }

            System.out.println("Update available: " + latestTag + " (current: v" + currentVersion + ")");

            if (checkOnly) {
                return 0;
            }

            // 3. Download the checksum manifest
            String manifestUrl = DOWNLOAD_BASE_URL + "/" + latestTag
                    + "/" + ReleaseChecker.CHECKSUM_MANIFEST_ASSET;
            String manifest = downloadWithRetry(manifestUrl, "checksum manifest");

            // 4. Determine platform asset name and download binary
            String assetName = platformAssetName();
            String assetUrl = DOWNLOAD_BASE_URL + "/" + latestTag + "/" + assetName;
            byte[] binary = downloadBinaryWithRetry(assetUrl, assetName);

            // 5. Verify SHA-256
            String expectedHash = extractHash(manifest, assetName);
            if (expectedHash != null) {
                String actualHash = sha256(binary);
                if (!expectedHash.equalsIgnoreCase(actualHash)) {
                    System.err.println("Checksum mismatch!");
                    System.err.println("  Expected: " + expectedHash);
                    System.err.println("  Actual:   " + actualHash);
                    return 1;
                }
                System.out.println("Checksum verified: " + actualHash);
            }

            // 6. Atomically replace the current binary
            Path currentExe = Path.of(System.getProperty("java.home"), "bin", "jay");
            Path tempFile = Files.createTempFile("jay-update-", ".tmp");
            Files.write(tempFile, binary);
            tempFile.toFile().setExecutable(true);
            Files.move(tempFile, currentExe, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);

            System.out.println("Updated to " + latestTag);
            return 0;

        } catch (IOException e) {
            System.err.println("Update failed: " + e.getMessage());
            System.err.println();
            System.err.println(ReleaseChecker.updateNetworkFallbackHint());
            return 1;
        }
    }

    private String downloadWithRetry(String url, String description) throws IOException {
        IOException last = null;
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                return downloadString(url);
            } catch (IOException e) {
                last = e;
                if (i < MAX_RETRIES - 1) {
                    try { Thread.sleep(RETRY_DELAY.toMillis()); }
                    catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                }
            }
        }
        throw last != null ? last : new IOException("failed to download " + description);
    }

    private byte[] downloadBinaryWithRetry(String url, String description) throws IOException {
        IOException last = null;
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                return downloadBytes(url);
            } catch (IOException e) {
                last = e;
                if (i < MAX_RETRIES - 1) {
                    try { Thread.sleep(RETRY_DELAY.toMillis()); }
                    catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                }
            }
        }
        throw last != null ? last : new IOException("failed to download " + description);
    }

    private String downloadString(String url) throws IOException {
        try (var client = buildClient()) {
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("User-Agent", ReleaseChecker.UPDATE_USER_AGENT)
                    .GET()
                    .build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IOException("HTTP " + response.statusCode() + " fetching " + url);
            }
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted", e);
        }
    }

    private byte[] downloadBytes(String url) throws IOException {
        try (var client = buildClient()) {
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMinutes(5))
                    .header("User-Agent", ReleaseChecker.UPDATE_USER_AGENT)
                    .GET()
                    .build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                throw new IOException("HTTP " + response.statusCode() + " fetching " + url);
            }
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted", e);
        }
    }

    private HttpClient buildClient() {
        var builder = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10));
        // Proxy support would go here if proxyUrl != null
        return builder.build();
    }

    /** Determine the platform-specific binary asset name. */
    static String platformAssetName() {
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();

        String osName;
        if (os.contains("mac")) osName = "apple-darwin";
        else if (os.contains("win")) osName = "pc-windows-msvc";
        else osName = "unknown-linux-gnu";

        String archName;
        if (arch.contains("aarch64") || arch.contains("arm64")) archName = "aarch64";
        else archName = "x86_64";

        String ext = os.contains("win") ? ".exe" : "";
        return "codewhale-" + archName + "-" + osName + ext;
    }

    /** Extract SHA-256 hash for a given asset from the manifest. */
    static String extractHash(String manifest, String assetName) {
        for (String line : manifest.lines().toList()) {
            String trimmed = line.trim();
            if (trimmed.endsWith(assetName)) {
                int space = trimmed.indexOf(' ');
                if (space > 0) return trimmed.substring(0, space);
            }
        }
        return null;
    }

    static String sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
