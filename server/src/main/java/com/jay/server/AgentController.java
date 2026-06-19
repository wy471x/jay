package com.jay.server;

import com.jay.protocol.PromptRequest;
import com.jay.protocol.PromptResponse;
import com.jay.protocol.EventFrame;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * REST controller for agent interactions.
 * Exposes endpoints for the Next.js web frontend.
 */
@RestController
@RequestMapping("/api/v1")
public class AgentController {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @PostMapping("/chat")
    public PromptResponse chat(@RequestBody PromptRequest request) {
        return new PromptResponse("Agent response placeholder", "claude-sonnet-4-6", List.of());
    }

    @GetMapping("/sse")
    public SseEmitter stream() {
        var emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        return emitter;
    }

    @GetMapping("/health")
    public java.util.Map<String, String> health() {
        return java.util.Map.of("status", "OK");
    }
}
