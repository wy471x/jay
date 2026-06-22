package com.jay.config.subconfig;

import java.util.List;

public class NetworkPolicyConfig {
    private String allow;
    private String deny;
    private List<String> proxy = List.of();

    public String allow() { return allow; }

    public void allow(String v) { allow = v; }

    public String deny() { return deny; }

    public void deny(String v) { deny = v; }

    public List<String> proxy() { return proxy; }

    public void proxy(List<String> v) { proxy = v; }
}
