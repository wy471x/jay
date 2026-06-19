package com.jay.core;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.jay.core.HookEvent.Type;

/**
 * Dispatches lifecycle hooks using Spring's ApplicationEventPublisher.
 * Replaces the hand-written Hook dispatcher from Rust (~500 lines)
 * with Spring's declarative event model (~300 lines).
 */
@Component
public class HookDispatcher {

    private final ApplicationEventPublisher publisher;

    public HookDispatcher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void dispatch(Type type, String sessionId, String payload) {
        publisher.publishEvent(new HookEvent(this, type, sessionId, payload));
    }

    public void onSessionStart(String sessionId) {
        dispatch(Type.SESSION_START, sessionId, "");
    }

    public void onSessionEnd(String sessionId) {
        dispatch(Type.SESSION_END, sessionId, "");
    }

    public void onPreToolUse(String sessionId, String toolName) {
        dispatch(Type.PRE_TOOL_USE, sessionId, toolName);
    }

    public void onPostToolUse(String sessionId, String toolName, String result) {
        dispatch(Type.POST_TOOL_USE, sessionId, toolName + ":" + result);
    }

    public void onUserPrompt(String sessionId, String prompt) {
        dispatch(Type.USER_PROMPT, sessionId, prompt);
    }

    public void onModelResponse(String sessionId, String modelId, String content) {
        dispatch(Type.MODEL_RESPONSE, sessionId, modelId + ":" + content);
    }
}
