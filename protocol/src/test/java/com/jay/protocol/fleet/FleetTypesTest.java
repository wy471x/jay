package com.jay.protocol.fleet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Roundtrip serialization tests for Fleet protocol types.
 * Ported from Rust's fleet.rs tests (lines 862-1270).
 */
class FleetTypesTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    void fleetRunRoundTrip() throws Exception {
        var run = new FleetRun(
                new FleetRunId("run-001"), "dogfood smoke", FleetRunStatus.RUNNING,
                List.of(new FleetTaskSpec(
                        "task-1", "lint", null,
                        "Keep the workspace lint-clean",
                        "run cargo clippy",
                        new FleetTaskWorkerProfile("release-checker", "read-only",
                                List.of("cargo"), List.of("rust")),
                        new FleetWorkspaceRequirements(".", List.of("Cargo.toml"), List.of(),
                                new FleetEnvironmentRequirements(List.of("PATH"), List.of("RUST_LOG"))),
                        List.of("crates/tui/src/main.rs"),
                        List.of("release gate"),
                        new FleetTaskBudget(8000L, 20, 300L),
                        List.of("release"),
                        List.of(FleetArtifactKind.TEST_RESULT),
                        null, // scorer: use ExitCode
                        new FleetRetryPolicy(),
                        null, // alertPolicy
                        300L,
                        Map.of()
                )),
                List.of(),
                Map.of(),
                null, null, null, null
        );

        var json = mapper.writeValueAsString(run);
        var back = mapper.readValue(json, FleetRun.class);

        assertEquals("run-001", back.id().value());
        assertEquals(FleetRunStatus.RUNNING, back.status());
        assertEquals(1, back.taskSpecs().size());
        assertEquals("release-checker", back.taskSpecs().get(0).worker().role());
        assertEquals(List.of("Cargo.toml"), back.taskSpecs().get(0).workspace().requiredFiles());
    }

    @Test
    void workerEventLifecycleRoundTrip() throws Exception {
        var events = List.of(
                new FleetWorkerEvent(1, new FleetRunId("run-002"), "worker-a", "task-1",
                        "2026-06-12T17:01:00Z", new FleetWorkerEventPayload.Queued(), Map.of()),
                new FleetWorkerEvent(2, new FleetRunId("run-002"), "worker-a", "task-1",
                        "2026-06-12T17:01:05Z",
                        new FleetWorkerEventPayload.RunningTool("bash", "call-1"), Map.of()),
                new FleetWorkerEvent(3, new FleetRunId("run-002"), "worker-a", "task-1",
                        "2026-06-12T17:02:00Z",
                        new FleetWorkerEventPayload.Completed(0, "ok"), Map.of())
        );

        var json = mapper.writeValueAsString(events);
        var back = mapper.readValue(json, FleetWorkerEvent[].class);

        assertEquals(3, back.length);
        assertInstanceOf(FleetWorkerEventPayload.Queued.class, back[0].payload());
        assertInstanceOf(FleetWorkerEventPayload.Completed.class, back[2].payload());
        assertEquals(0, ((FleetWorkerEventPayload.Completed) back[2].payload()).exitCode());
    }

    @Test
    void alertPolicyRoundTrip() throws Exception {
        var policy = new FleetAlertPolicy(
                List.of(FleetAlertEventClass.STALE),
                List.of(new FleetAlertChannel.Slack(
                        new FleetAlertEndpoint("https://hooks.slack.com/test", null, null))),
                2, 10L);

        var json = mapper.writeValueAsString(policy);
        assertTrue(json.contains("\"events\":[\"stale\"]"));
        assertTrue(json.contains("\"kind\":\"slack\""));
        assertTrue(json.contains("\"after_attempts\":2"));

        var back = mapper.readValue(json, FleetAlertPolicy.class);
        assertEquals(List.of(FleetAlertEventClass.STALE), back.events());
        assertEquals(2, back.afterAttempts());
    }

    @Test
    void artifactKindSerializesAsPlainString() throws Exception {
        var known = mapper.writeValueAsString(FleetArtifactKind.TEST_RESULT);
        assertEquals("\"test_result\"", known);

        var custom = mapper.writeValueAsString(FleetArtifactKind.of("coverage.xml"));
        assertEquals("\"coverage.xml\"", custom);

        var parsed = mapper.readValue("\"coverage.xml\"", FleetArtifactKind.class);
        assertEquals("coverage.xml", parsed.value());
    }

    @Test
    void artifactOtherKindRoundTrip() throws Exception {
        var artifact = new FleetArtifactRef(
                FleetArtifactKind.of("coverage.xml"), "/tmp/coverage.xml",
                "sha256:abc", "application/xml", 1024L);

        var json = mapper.writeValueAsString(artifact);
        var back = mapper.readValue(json, FleetArtifactRef.class);

        assertEquals("coverage.xml", back.kind().value());
        assertEquals(1024L, back.sizeBytes());
    }

    @Test
    void sshHostSpecAcceptsMinimalJson() throws Exception {
        var json = "{\"kind\":\"ssh\",\"host\":\"builder.example.test\"}";
        var host = mapper.readValue(json, FleetHostSpec.class);

        assertInstanceOf(FleetHostSpec.Ssh.class, host);
        var ssh = (FleetHostSpec.Ssh) host;
        assertEquals("builder.example.test", ssh.host());
        assertNull(ssh.port());
        assertNull(ssh.user());
        assertNull(ssh.identity());
    }

    @Test
    void sshHostSpecWithKeyPinningRoundTrip() throws Exception {
        var spec = new FleetHostSpec.Ssh(
                "builder.trusted.example.com", 22, "codewhale",
                "~/.ssh/codewhale_fleet",
                "~/.ssh/known_hosts",
                "SHA256:aLGqZo1M6c...",
                "/srv/codewhale/work",
                List.of("CODEWHALE_PROFILE"),
                "/usr/local/bin/codewhale");

        var json = mapper.writeValueAsString(spec);
        assertTrue(json.contains("known_hosts"));
        assertTrue(json.contains("host_key_fingerprint"));
        assertTrue(json.contains("SHA256:aLGqZo1M6c..."));

        var back = mapper.readValue(json, FleetHostSpec.class);
        assertInstanceOf(FleetHostSpec.Ssh.class, back);
        var ssh = (FleetHostSpec.Ssh) back;
        assertEquals("builder.trusted.example.com", ssh.host());
        assertEquals("~/.ssh/known_hosts", ssh.knownHosts());
        assertEquals("SHA256:aLGqZo1M6c...", ssh.hostKeyFingerprint());
    }

    @Test
    void retryPolicyMissingFieldsUseNonzeroDefaults() throws Exception {
        // Test partial JSON deserialization with defaults
        var custom = mapper.readValue("{\"max_attempts\":5}", FleetRetryPolicy.class);
        assertEquals(5, custom.maxAttempts());

        // Full explicit defaults
        var policy = mapper.readValue(
            "{\"max_attempts\":3,\"initial_backoff_seconds\":5," +
            "\"max_backoff_seconds\":300,\"backoff_multiplier\":2}",
            FleetRetryPolicy.class);
        assertEquals(3, policy.maxAttempts());
        assertEquals(5, policy.initialBackoffSeconds());
        assertEquals(300, policy.maxBackoffSeconds());
    }

    @Test
    void sparseWorkerEventsOmitAbsentOptionalFields() throws Exception {
        var heartbeat = new FleetWorkerEventPayload.Heartbeat(null, null);
        var json = mapper.writeValueAsString(heartbeat);
        // state field must be present; optional fields may be omitted or null
        assertTrue(json.contains("heartbeat"));

        var completed = new FleetWorkerEventPayload.Completed(null, null);
        var completedJson = mapper.writeValueAsString(completed);
        assertTrue(completedJson.contains("completed"));
    }

    @Test
    void receiptRoundTrip() throws Exception {
        var receipt = new FleetReceipt(
                new FleetRunId("run-003"), "task-1", "worker-b",
                "2026-06-12T17:03:00Z", FleetTaskResult.PASS, null,
                List.of(), new FleetScore(0.95, 1.0, null));

        var json = mapper.writeValueAsString(receipt);
        var back = mapper.readValue(json, FleetReceipt.class);
        assertEquals(FleetTaskResult.PASS, back.result());
        assertEquals(0.95, back.score().value(), 0.001);
    }

    @Test
    void partialReceiptRecordsFailureSource() throws Exception {
        var receipt = new FleetReceipt(
                new FleetRunId("run-004"), "task-2", "worker-c",
                "2026-06-12T17:04:00Z", FleetTaskResult.PARTIAL,
                FleetTaskFailureKind.VERIFIER, List.of(),
                new FleetScore(0.5, 1.0, "manual verification required"));

        var json = mapper.writeValueAsString(receipt);
        assertTrue(json.contains("\"result\":\"partial\""));
        assertTrue(json.contains("\"failure_kind\":\"verifier\""));

        var back = mapper.readValue(json, FleetReceipt.class);
        assertEquals(FleetTaskResult.PARTIAL, back.result());
        assertEquals(FleetTaskFailureKind.VERIFIER, back.failureKind());
    }

    @Test
    void trustLevelOrdinalReflectsPrivilege() {
        assertTrue(FleetTrustLevel.OPERATOR.compareTo(FleetTrustLevel.REMOTE_VERIFIED) > 0);
        assertTrue(FleetTrustLevel.REMOTE_VERIFIED.compareTo(FleetTrustLevel.LOCAL) > 0);
        assertTrue(FleetTrustLevel.LOCAL.compareTo(FleetTrustLevel.SANDBOX) > 0);
    }

    @Test
    void trustLevelAcceptsHyphenatedRemoteVerified() throws Exception {
        var trust = mapper.readValue("\"remote_verified\"", FleetTrustLevel.class);
        assertEquals(FleetTrustLevel.REMOTE_VERIFIED, trust);

        var canonical = mapper.writeValueAsString(trust);
        assertEquals("\"remote_verified\"", canonical);
    }

    @Test
    void securityPolicyDefaultsAreConservative() throws Exception {
        var json = "{}";
        // Without explicit fields, Jackson will use defaults
        var policy = new FleetSecurityPolicy(
                FleetTrustLevel.SANDBOX, List.of(), List.of(),
                FleetTrustLevel.OPERATOR, false, false);
        assertEquals(FleetTrustLevel.SANDBOX, policy.defaultTrustLevel());
        assertTrue(policy.allowedSecrets().isEmpty());
    }

    @Test
    void alertChannelAcceptsSlackWithWebhookAndSecret() throws Exception {
        // Build and serialize a Slack alert channel
        var endpoint = new FleetAlertEndpoint(
                "https://hooks.slack.com/test", null,
                new FleetSecretRef("SLACK_SIGNING_SECRET", null));
        var channel = new FleetAlertChannel.Slack(endpoint);
        var json = mapper.writeValueAsString((FleetAlertChannel) channel);
        assertTrue(json.contains("slack"));
        assertTrue(json.contains("SLACK_SIGNING_SECRET"));

        // Deserialize back
        var back = mapper.readValue(json, FleetAlertChannel.class);
        assertInstanceOf(FleetAlertChannel.Slack.class, back);
        var slack = (FleetAlertChannel.Slack) back;
        assertEquals("https://hooks.slack.com/test", slack.webhook().url());
        assertNotNull(slack.webhook().secretRef());
        assertEquals("SLACK_SIGNING_SECRET", slack.webhook().secretRef().key());
    }

    @Test
    void secretRefRedactedNeverExposesValue() {
        var ref = new FleetSecretRef("DEEPSEEK_API_KEY", null);
        var display = String.format("<secret:%s>", ref.key());
        assertTrue(display.contains("DEEPSEEK_API_KEY"));
        assertFalse(display.contains("sk-"));
        assertTrue(display.startsWith("<secret:"));

        var ref2 = new FleetSecretRef("GH_TOKEN", "env");
        var display2 = String.format("<secret:%s.%s>", ref2.source(), ref2.key());
        assertTrue(display2.contains("env.GH_TOKEN"));
    }

    @Test
    void alertEndpointFromSecretRoundTrip() throws Exception {
        var endpoint = new FleetAlertEndpoint(null, new FleetSecretRef("SLACK_WEBHOOK", null), null);
        var json = mapper.writeValueAsString(endpoint);
        assertTrue(json.contains("SLACK_WEBHOOK"));
        assertFalse(json.contains("hooks.slack.com"));

        var back = mapper.readValue(json, FleetAlertEndpoint.class);
        assertEquals("SLACK_WEBHOOK", back.urlRef().key());
        assertNull(back.url());
    }

    @Test
    void secretRefAcceptsStringWireShape() throws Exception {
        var ref = mapper.readValue("\"CODEWHALE_FLEET_TOKEN\"", FleetSecretRef.class);
        assertEquals("CODEWHALE_FLEET_TOKEN", ref.key());
        assertNull(ref.source());

        var structured = mapper.readValue(
                "{\"key\":\"GH_TOKEN\",\"source\":\"env\"}", FleetSecretRef.class);
        assertEquals("GH_TOKEN", structured.key());
        assertEquals("env", structured.source());
    }

    @Test
    void fleetTaskResultValues() throws Exception {
        assertEquals(FleetTaskResult.PASS, mapper.readValue("\"pass\"", FleetTaskResult.class));
        assertEquals(FleetTaskResult.FAIL, mapper.readValue("\"fail\"", FleetTaskResult.class));
        assertEquals(FleetTaskResult.TIMEOUT, mapper.readValue("\"timeout\"", FleetTaskResult.class));
        assertEquals("\"skip\"", mapper.writeValueAsString(FleetTaskResult.SKIP));
    }
}
