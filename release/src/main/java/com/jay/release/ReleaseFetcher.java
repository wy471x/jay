package com.jay.release;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * HTTP fetching of release metadata from GitHub API or mirrors.
 * Equivalent to Rust's fetch_release_json_blocking / fetch_release_json_async.
 */
final class ReleaseFetcher {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String USER_AGENT = "codewhale-updater";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private ReleaseFetcher() {}

    /** Fetch a JSON string from a URL with a 5-second timeout. */
    static String fetchJson(String url, String description) throws IOException {
        try (var client = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build()) {
            var request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/vnd.github+json")
                .GET()
                .build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                throw new IOException("HTTP " + status + " fetching " + description + " from " + url);
            }
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted fetching " + description + " from " + url, e);
        }
    }

    /** Extract tag_name from a single-release JSON response. */
    static String parseLatestTag(String body) throws IOException {
        JsonNode root = mapper.readTree(body);
        JsonNode tag = root.get("tag_name");
        if (tag == null || tag.isNull()) {
            throw new IOException("missing tag_name in release JSON");
        }
        return tag.asText();
    }

    /** Extract the first beta tag_name from a release-list JSON response. */
    static String parseLatestBetaTag(String body) throws IOException {
        JsonNode array = mapper.readTree(body);
        if (!array.isArray()) {
            throw new IOException("expected JSON array for release list");
        }
        for (JsonNode entry : array) {
            JsonNode tag = entry.get("tag_name");
            if (tag != null && isBetaTag(tag.asText())) {
                return tag.asText();
            }
        }
        throw new IOException("no beta release found in release list");
    }

    static boolean isBetaTag(String tagName) {
        return tagName != null && tagName.toLowerCase().contains("beta");
    }
}
