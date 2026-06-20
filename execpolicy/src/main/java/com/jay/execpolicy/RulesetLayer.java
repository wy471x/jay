package com.jay.execpolicy;

public enum RulesetLayer {
    BuiltinDefault(0),
    Agent(1),
    User(2);

    private final int priority;

    RulesetLayer(int priority) { this.priority = priority; }

    public int priority() { return priority; }
}
