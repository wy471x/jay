package com.jay.config.subconfig;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SnapshotsConfig {
    private boolean enabled = true;
    @JsonProperty("max_age_days") private int maxAgeDays = 7;

    public boolean enabled() { return enabled; }

    public void enabled(boolean v) { enabled = v; }

    public int maxAgeDays() { return maxAgeDays; }

    public void maxAgeDays(int v) { maxAgeDays = v; }
}
