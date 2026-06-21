package com.jay.state.model;

public record ThreadListFilters(boolean includeArchived, int limit) {
    public ThreadListFilters {
        if (limit <= 0) limit = 50;
    }

    public static ThreadListFilters defaults() {
        return new ThreadListFilters(false, 50);
    }
}
