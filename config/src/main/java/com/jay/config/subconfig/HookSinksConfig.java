package com.jay.config.subconfig;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HookSinksConfig {
    @JsonProperty("unix_socket_path") private String unixSocketPath;

    public String unixSocketPath() { return unixSocketPath; }

    public void unixSocketPath(String v) { unixSocketPath = v; }
}
