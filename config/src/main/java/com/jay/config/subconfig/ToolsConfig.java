package com.jay.config.subconfig;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ToolsConfig {
    @JsonProperty("always_load") private List<String> alwaysLoad = List.of();

    public List<String> alwaysLoad() { return alwaysLoad; }

    public void alwaysLoad(List<String> v) { alwaysLoad = v; }
}
