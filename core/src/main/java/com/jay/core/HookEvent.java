package com.jay.core;

import org.springframework.context.ApplicationEvent;

/**
 * Spring ApplicationEvent representing a lifecycle hook event.
 * Listeners use @EventListener to react to specific hook types.
 */
public class HookEvent extends ApplicationEvent {

    private final Type type;
    private final String sessionId;
    private final String payload;

    public HookEvent(Object source, Type type, String sessionId, String payload) {
        super(source);
        this.type = type;
        this.sessionId = sessionId;
        this.payload = payload;
    }

    public Type type() { return type; }
    public String sessionId() { return sessionId; }
    public String payload() { return payload; }

    public enum Type {
        SESSION_START,
        SESSION_END,
        PRE_TOOL_USE,
        POST_TOOL_USE,
        USER_PROMPT,
        MODEL_RESPONSE
    }
}
