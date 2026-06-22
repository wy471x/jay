package com.jay.jayflow.validation;

import java.nio.file.Path;
import java.util.List;

/** File scope overlap detection. Equivalent to Rust's scopes_overlap + normalize_scope. */
public final class ScopeOverlap {
    private ScopeOverlap() { }

    public static boolean overlaps(List<String> left, List<String> right) {
        return left.stream().anyMatch(ls ->
                right.stream().anyMatch(rs -> scopeOverlaps(ls, rs)));
    }

    private static boolean scopeOverlaps(String left, String right) {
        var l = normalize(left); var r = normalize(right);
        if (l.equals(r) || l.equals(".") || r.equals(".")) return true;
        if (l.contains("*") || r.contains("*"))
            return globPrefix(l).equals(globPrefix(r));
        return Path.of(l).startsWith(r) || Path.of(r).startsWith(l);
    }

    private static String normalize(String scope) {
        var s = scope.trim();
        if (s.startsWith("./")) s = s.substring(2);
        if (s.endsWith("/**")) s = s.substring(0, s.length() - 3);
        else if (s.endsWith("/*")) s = s.substring(0, s.length() - 2);
        s = s.replaceAll("/$", "");
        return s;
    }

    private static String globPrefix(String scope) {
        int star = scope.indexOf('*');
        var pre = star >= 0 ? scope.substring(0, star) : scope;
        return pre.replaceAll("/$", "");
    }
}
