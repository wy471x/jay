package com.jay.core.thread;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

public sealed interface InitialHistory {
    record New() implements InitialHistory { }

    record Forked(List<JsonNode> items) implements InitialHistory { }

    record Resumed(String conversationId, List<JsonNode> history, String rolloutPath) implements InitialHistory { }

    static InitialHistory forked(String parentId) {
        var node = new ObjectMapper().createObjectNode()
            .put("type", "fork")
            .put("parent_thread_id", parentId);
        return new Forked(List.of(node));
    }
}
