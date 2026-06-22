package com.jay.release;

public sealed interface ReleaseQuery {
    record Mirror(String baseUrl, String version) implements ReleaseQuery { }

    record GitHubLatest(String url) implements ReleaseQuery { }

    record GitHubReleaseList(String url) implements ReleaseQuery { }
}
