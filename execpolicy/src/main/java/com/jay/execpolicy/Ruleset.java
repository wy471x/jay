package com.jay.execpolicy;

import java.util.ArrayList;
import java.util.List;

public class Ruleset {
    private final RulesetLayer layer;
    private final List<String> trustedPrefixes;
    private final List<String> deniedPrefixes;
    private final List<ToolAskRule> askRules;

    public Ruleset(RulesetLayer layer, List<String> trustedPrefixes, List<String> deniedPrefixes) {
        this(layer, trustedPrefixes, deniedPrefixes, List.of());
    }

    public Ruleset(RulesetLayer layer, List<String> trustedPrefixes, List<String> deniedPrefixes,
                   List<ToolAskRule> askRules) {
        this.layer = layer;
        this.trustedPrefixes = List.copyOf(trustedPrefixes);
        this.deniedPrefixes = List.copyOf(deniedPrefixes);
        this.askRules = List.copyOf(askRules);
    }

    public static Ruleset builtinDefault() {
        return new Ruleset(RulesetLayer.BuiltinDefault, List.of(), List.of());
    }

    public static Ruleset agent(List<String> trusted, List<String> denied) {
        return new Ruleset(RulesetLayer.Agent, trusted, denied);
    }

    public static Ruleset user(List<String> trusted, List<String> denied) {
        return new Ruleset(RulesetLayer.User, trusted, denied);
    }

    public Ruleset withAskRules(List<ToolAskRule> askRules) {
        return new Ruleset(layer, trustedPrefixes, deniedPrefixes, askRules);
    }

    public RulesetLayer layer() { return layer; }
    public List<String> trustedPrefixes() { return trustedPrefixes; }
    public List<String> deniedPrefixes() { return deniedPrefixes; }
    public List<ToolAskRule> askRules() { return askRules; }
}
