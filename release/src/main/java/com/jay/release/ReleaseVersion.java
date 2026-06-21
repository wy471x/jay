package com.jay.release;

import java.util.regex.Pattern;

/**
 * Lightweight semver parsing and comparison.
 * Equivalent to Rust's semver::Version + parse_release_version.
 */
public record ReleaseVersion(int major, int minor, int patch, String preRelease) implements Comparable<ReleaseVersion> {

    private static final Pattern SEMVER_PATTERN =
        Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)(?:-(.+))?$");

    /**
     * Parse a version string. Strips leading 'v', trailing build metadata
     * (space-delimited), and whitespace.
     */
    public static ReleaseVersion parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("empty version string");
        }
        String cleaned = value.trim();
        if (cleaned.startsWith("v") || cleaned.startsWith("V")) {
            cleaned = cleaned.substring(1);
        }
        // Strip trailing build metadata (e.g. "1.0.0 (abc123)")
        int space = cleaned.indexOf(' ');
        if (space > 0) {
            cleaned = cleaned.substring(0, space);
        }
        // Strip parenthesized suffix
        int paren = cleaned.indexOf('(');
        if (paren > 0) {
            cleaned = cleaned.substring(0, paren).trim();
        }

        var matcher = SEMVER_PATTERN.matcher(cleaned);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("invalid semver: " + value);
        }
        return new ReleaseVersion(
            Integer.parseInt(matcher.group(1)),
            Integer.parseInt(matcher.group(2)),
            Integer.parseInt(matcher.group(3)),
            matcher.group(4) // may be null
        );
    }

    public boolean isBeta() {
        return preRelease != null && preRelease.toLowerCase().contains("beta");
    }

    /** Strip 'v' prefix from a tag name for comparison. */
    public static String stripVPrefix(String tag) {
        if (tag == null) return "";
        String t = tag.trim();
        return (t.startsWith("v") || t.startsWith("V")) ? t.substring(1) : t;
    }

    @Override
    public int compareTo(ReleaseVersion other) {
        int cmp = Integer.compare(major, other.major);
        if (cmp != 0) return cmp;
        cmp = Integer.compare(minor, other.minor);
        if (cmp != 0) return cmp;
        cmp = Integer.compare(patch, other.patch);
        if (cmp != 0) return cmp;
        // Pre-release versions sort before release versions
        if (preRelease == null && other.preRelease == null) return 0;
        if (preRelease == null) return 1;
        if (other.preRelease == null) return -1;
        return preRelease.compareTo(other.preRelease);
    }

    @Override
    public String toString() {
        var sb = new StringBuilder().append(major).append('.').append(minor).append('.').append(patch);
        if (preRelease != null) sb.append('-').append(preRelease);
        return sb.toString();
    }
}
