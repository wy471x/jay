package com.jay.server;

import com.jay.agent.ModelRegistry;
import com.jay.execpolicy.ExecPolicyEngine;
import com.jay.server.controller.AppController;
import com.jay.server.controller.ChatCompletionsController;
import com.jay.tools.ToolRegistry;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ServerTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        var modelRegistry = new ModelRegistry(List.of());
        var toolRegistry = new ToolRegistry();
        var policyEngine = new ExecPolicyEngine(List.of(), List.of());
        var restClient = RestClient.create();

        mvc = MockMvcBuilders.standaloneSetup(
                new AppController(modelRegistry, toolRegistry, policyEngine),
                new ChatCompletionsController(restClient, modelRegistry))
            .build();
    }

    // ── Health ──────────────────────────────────────────────────────

    @Test
    void healthzReturnsOk() throws Exception {
        mvc.perform(get("/healthz"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"))
            .andExpect(jsonPath("$.service").value("jay-app-server"));
    }

    // ── Thread ──────────────────────────────────────────────────────

    @Test
    void threadAcceptsCreate() throws Exception {
        mvc.perform(post("/thread")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"kind\":\"create\",\"metadata\":{}}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ok").value(true));
    }

    // ── App Capabilities ────────────────────────────────────────────

    @Test
    void appCapabilitiesReturnsRoutes() throws Exception {
        mvc.perform(post("/app")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"kind\":\"capabilities\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.service").value("jay-app-server"))
            .andExpect(jsonPath("$.routes").isArray());
    }

    @Test
    void appConfigGetReturnsRedactedValue() throws Exception {
        mvc.perform(post("/app")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"kind\":\"config_get\",\"key\":\"api.key\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.redacted").value(true));
    }

    @Test
    void appConfigListReturnsItems() throws Exception {
        mvc.perform(post("/app")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"kind\":\"config_list\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.config").isArray());
    }

    // ── Prompt ───────────────────────────────────────────────────────

    @Test
    void promptReturnsResponse() throws Exception {
        mvc.perform(post("/prompt")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"prompt\":\"hello\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.output").exists());
    }

    // ── Tool ─────────────────────────────────────────────────────────

    @Test
    void toolMissingCallReturnsBadRequest() throws Exception {
        mvc.perform(post("/tool")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }

    // ── Jobs ─────────────────────────────────────────────────────────

    @Test
    void jobsReturnsOk() throws Exception {
        mvc.perform(get("/jobs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ok").value(true))
            .andExpect(jsonPath("$.jobs").isArray());
    }

    // ── MCP Startup ──────────────────────────────────────────────────

    @Test
    void mcpStartupReturnsSummary() throws Exception {
        mvc.perform(post("/mcp/startup"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ok").value(true))
            .andExpect(jsonPath("$.summary").exists());
    }

    // ── Chat Completions ────────────────────────────────────────────

    @Test
    void chatCompletionsRejectsStreaming() throws Exception {
        mvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"model\":\"claude-sonnet-4-6\",\"stream\":true}"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("streaming_unsupported")));
    }

    @Test
    void chatCompletionsAcceptsRequestWithFallbackModel() throws Exception {
        // Registry falls back to a default model. Verify endpoint
        // processes the request (may fail upstream but not with 400).
        var result = mvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"model\":\"\",\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}]}"))
            .andReturn();
        assertNotEquals(400, result.getResponse().getStatus());
    }
}
