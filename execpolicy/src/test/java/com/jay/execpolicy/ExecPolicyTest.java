package com.jay.execpolicy;

import com.jay.protocol.approval.AskForApproval;
import com.jay.protocol.approval.NetworkPolicyRuleAction;
import org.junit.jupiter.api.*;

import java.util.*;

import static com.jay.execpolicy.BashArityDict.*;
import static org.junit.jupiter.api.Assertions.*;

class ExecPolicyTest {

    // ── BashArityDict classify ────────────────────────────────────────

    @Nested
    class BashArityDictClassify {

        private final BashArityDict dict = new BashArityDict();

        @Test
        void classifyGitStatus() {
            assertEquals("git status", dict.classify(tokens("git status")));
            assertEquals("git status", dict.classify(tokens("git status -s")));
            assertEquals("git status", dict.classify(tokens("git status --porcelain")));
        }

        @Test
        void classifyGitPush() {
            assertEquals("git push", dict.classify(tokens("git push origin main")));
            assertEquals("git push", dict.classify(tokens("git push --force")));
        }

        @Test
        void classifyNpmRun() {
            assertEquals("npm run dev", dict.classify(tokens("npm run dev")));
            assertEquals("npm test", dict.classify(tokens("npm test -- --coverage")));
        }

        @Test
        void classifyDockerCompose() {
            assertEquals("docker compose up", dict.classify(tokens("docker compose up -d")));
            assertEquals("docker run", dict.classify(tokens("docker run --rm -it alpine")));
        }

        @Test
        void classifyKubectl() {
            assertEquals("kubectl get pods", dict.classify(tokens("kubectl get pods -n default")));
            assertEquals("kubectl apply", dict.classify(tokens("kubectl apply -f manifest.yaml")));
        }

        @Test
        void classifyGoMod() {
            assertEquals("go mod tidy", dict.classify(tokens("go mod tidy")));
            assertEquals("go test", dict.classify(tokens("go test ./... -v")));
        }

        @Test
        void classifyMake() {
            assertEquals("make", dict.classify(tokens("make")));
            assertEquals("make", dict.classify(tokens("make build")));
            assertEquals("make", dict.classify(tokens("make -j4")));
        }

        @Test
        void classifyAwsS3() {
            assertEquals("aws s3 ls", dict.classify(tokens("aws s3 ls s3://bucket")));
        }

        @Test
        void classifyTerraform() {
            assertEquals("terraform plan", dict.classify(tokens("terraform plan -out=tfplan")));
        }

        @Test
        void classifyUnknownCommandFallsBackToBase() {
            assertEquals("myapp", dict.classify(tokens("myapp --verbose do-stuff")));
        }

        @Test
        void classifyEmptyOrAllFlags() {
            assertEquals("", dict.classify(List.of()));
            assertEquals("", dict.classify(tokens("-v --help")));
        }

        private static List<String> tokens(String cmd) {
            return Arrays.asList(cmd.split(" "));
        }
    }

    // ── BashArityDict allowRuleMatches ─────────────────────────────────

    @Nested
    class BashArityDictAllowRuleMatches {

        private final BashArityDict dict = new BashArityDict();

        @Test
        void gitStatusMatchesGitStatusMinusS() {
            assertTrue(dict.allowRuleMatches("git status", "git status -s"));
        }

        @Test
        void gitStatusDoesNotMatchGitPush() {
            assertFalse(dict.allowRuleMatches("git status", "git push origin main"));
        }

        @Test
        void npmRunDevDoesNotMatchNpmRunBuild() {
            assertTrue(dict.allowRuleMatches("npm run dev", "npm run dev --watch"));
            assertFalse(dict.allowRuleMatches("npm run dev", "npm run build"));
        }

        @Test
        void lsMatchesUnregisteredPattern() {
            assertTrue(dict.allowRuleMatches("ls", "ls -la"));
            assertFalse(dict.allowRuleMatches("ls", "lsof"));
        }

        @Test
        void makeMatchesAnyTarget() {
            assertTrue(dict.allowRuleMatches("make", "make build"));
            assertTrue(dict.allowRuleMatches("make", "make test"));
        }

        @Test
        void dictCoversAtLeast30Commands() {
            assertTrue(dict.size() >= 30);
        }
    }

    // ── Helper functions ──────────────────────────────────────────────

    @Nested
    class HelperFunctions {

        @Test
        void normalizeCommandCollapsesWhitespace() {
            assertEquals("git status", normalizeCommand("  git   status  "));
            assertEquals("cargo test", normalizeCommand("Cargo Test"));
        }

        @Test
        void firstTokenExtracts() {
            assertEquals("cargo", firstToken("cargo test --release"));
            assertEquals("git", firstToken("git"));
        }

        @Test
        void normalizePathValueHandlesBackslashes() {
            assertEquals("foo/bar", normalizePathValue("\\foo\\bar\\"));
            assertEquals("foo/bar", normalizePathValue("/FOO/BAR/"));
            assertEquals("foo/bar", normalizePathValue("  /foo/bar/  "));
        }
    }

    // ── ToolAskRule ───────────────────────────────────────────────────

    @Nested
    class ToolAskRuleTests {

        @Test
        void labelIncludesAllFields() {
            assertEquals("tool=exec_shell", new ToolAskRule("exec_shell").label());
            assertEquals("tool=exec_shell command=cargo test",
                ToolAskRule.execShell("cargo test").label());
            assertEquals("tool=edit_file path=/tmp/project",
                ToolAskRule.filePath("edit_file", "/tmp/project").label());
        }

        @Test
        void specificityScoresPreferMoreSpecific() {
            var simple = new ToolAskRule("exec_shell");
            var withCmd = ToolAskRule.execShell("cargo test");
            var withPath = ToolAskRule.filePath("edit_file", "/tmp/project");
            assertTrue(withCmd.specificity() > simple.specificity());
            assertTrue(withPath.specificity() > simple.specificity());
        }
    }

    // ── ExecPolicyEngine ──────────────────────────────────────────────

    @Nested
    class ExecPolicyEngineTests {

        private ExecPolicyContext ctx(String command) {
            return new ExecPolicyContext(command, "/workspace", null, null,
                new AskForApproval.UnlessTrusted(), null);
        }

        private ExecPolicyContext ctx(String command, AskForApproval mode) {
            return new ExecPolicyContext(command, "/workspace", null, null, mode, null);
        }

        private ExecPolicyContext ctxWithTool(String command, String tool) {
            return new ExecPolicyContext(command, "/workspace", tool, null,
                new AskForApproval.UnlessTrusted(), null);
        }

        // ── Trusted / Deny ────────────────────────────────────────

        @Test
        void trustedPrefixSkipsApprovalWhenPolicyIsUnlessTrusted() {
            var engine = new ExecPolicyEngine(List.of("git status"), List.of());
            var decision = engine.check(ctx("git status -s"));
            assertTrue(decision.allow());
            assertFalse(decision.requiresApproval());
            assertInstanceOf(ExecApprovalRequirement.Skip.class, decision.requirement());
            assertEquals("git status", decision.matchedRule());
        }

        @Test
        void deniedPrefixBlocksEvenWhenCommandIsAlsoTrusted() {
            var engine = new ExecPolicyEngine(List.of("git status"), List.of("git status"));
            var decision = engine.check(ctx("git status -s"));
            assertFalse(decision.allow());
            assertFalse(decision.requiresApproval());
            assertInstanceOf(ExecApprovalRequirement.Forbidden.class, decision.requirement());
        }

        @Test
        void unmatchedCommandRequiresApprovalAndProposesFirstTokenRule() {
            var engine = new ExecPolicyEngine(List.of(), List.of());
            var decision = engine.check(ctx("cargo test"));
            assertTrue(decision.allow());
            assertTrue(decision.requiresApproval());
            var req = (ExecApprovalRequirement.NeedsApproval) decision.requirement();
            assertNotNull(req.proposedExecpolicyAmendment());
            assertTrue(req.proposedExecpolicyAmendment().prefixes().contains("cargo"));
            assertEquals(1, req.proposedNetworkPolicyAmendments().size());
            assertEquals("/workspace", req.proposedNetworkPolicyAmendments().get(0).host());
            assertEquals(NetworkPolicyRuleAction.ALLOW, req.proposedNetworkPolicyAmendments().get(0).action());
        }

        @Test
        void trustedCommandInOnRequestModeStillRequiresApproval() {
            var engine = new ExecPolicyEngine(List.of("git status"), List.of());
            var decision = engine.check(ctx("git status -s", new AskForApproval.OnRequest()));
            assertTrue(decision.allow());
            assertTrue(decision.requiresApproval());
            // OnRequest with trusted: needs approval, but no amendment proposed
            var req = (ExecApprovalRequirement.NeedsApproval) decision.requirement();
            assertNull(req.proposedExecpolicyAmendment());
        }

        @Test
        void rejectRulesModeForbidsUnmatchedCommand() {
            var engine = new ExecPolicyEngine(List.of(), List.of());
            var decision = engine.check(ctx("rm -rf /",
                new AskForApproval.Reject(false, true, false)));
            assertFalse(decision.allow());
            assertFalse(decision.requiresApproval());
            assertInstanceOf(ExecApprovalRequirement.Forbidden.class, decision.requirement());
        }

        @Test
        void onFailureModeSkipsApproval() {
            var engine = new ExecPolicyEngine(List.of(), List.of());
            var decision = engine.check(ctx("cargo test", new AskForApproval.OnFailure()));
            assertTrue(decision.allow());
            assertFalse(decision.requiresApproval());
            assertInstanceOf(ExecApprovalRequirement.Skip.class, decision.requirement());
        }

        // ── Typed ask rules ────────────────────────────────────────

        @Test
        void typedAskRuleRequiresApprovalUnderUnlessTrusted() {
            var ruleset = Ruleset.agent(List.of(), List.of()).withAskRules(
                List.of(ToolAskRule.execShell("cargo test")));
            var engine = new ExecPolicyEngine(List.of(ruleset), List.of(), List.of());
            var decision = engine.check(ctx("cargo test --release"));
            assertTrue(decision.allow());
            assertTrue(decision.requiresApproval());
            assertEquals("ask rule: " + ToolAskRule.execShell("cargo test").label(),
                decision.requirement().reason());
            // Typed ask rules produce NO network amendments
            var req = (ExecApprovalRequirement.NeedsApproval) decision.requirement();
            assertTrue(req.proposedNetworkPolicyAmendments().isEmpty());
        }

        @Test
        void typedAskRuleRequiresApprovalUnderOnFailure() {
            var ruleset = Ruleset.agent(List.of(), List.of()).withAskRules(
                List.of(ToolAskRule.execShell("cargo test")));
            var engine = new ExecPolicyEngine(List.of(ruleset), List.of(), List.of());
            var decision = engine.check(ctx("cargo test --release", new AskForApproval.OnFailure()));
            assertTrue(decision.allow());
            assertTrue(decision.requiresApproval());
        }

        @Test
        void typedAskRuleForbidsMatchingCommandWhenPolicyIsNever() {
            var ruleset = Ruleset.agent(List.of(), List.of()).withAskRules(
                List.of(ToolAskRule.execShell("cargo test")));
            var engine = new ExecPolicyEngine(List.of(ruleset), List.of(), List.of());
            var decision = engine.check(ctx("cargo test --release", new AskForApproval.Never()));
            assertFalse(decision.allow());
            assertFalse(decision.requiresApproval());
            assertInstanceOf(ExecApprovalRequirement.Forbidden.class, decision.requirement());
        }

        @Test
        void typedAskRuleOverridesTrustedButNotDeny() {
            var ruleset = Ruleset.agent(List.of(), List.of()).withAskRules(
                List.of(ToolAskRule.execShell("git status")));
            var engine = new ExecPolicyEngine(List.of(ruleset),
                List.of("git status"), List.of());
            // With ask rule + nothing denied, ask rule triggers approval
            var decision = engine.check(ctx("git status -s"));
            assertTrue(decision.allow());
            assertTrue(decision.requiresApproval());

            // With deny, deny wins
            var engine2 = new ExecPolicyEngine(List.of(ruleset),
                List.of("git status"), List.of("git status"));
            var decision2 = engine2.check(ctx("git status -s"));
            assertFalse(decision2.allow());
            assertFalse(decision2.requiresApproval());
            assertInstanceOf(ExecApprovalRequirement.Forbidden.class, decision2.requirement());
        }

        @Test
        void typedAskRulePrefersHigherLayerBeforeSpecificity() {
            var userRule = new ToolAskRule("exec_shell", "cargo", null);
            var agentRule = new ToolAskRule("exec_shell", "cargo test --release", null);
            // Agent rule is more specific, but User layer wins
            assertTrue(agentRule.specificity() > userRule.specificity());

            var agentRuleset = Ruleset.agent(List.of(), List.of()).withAskRules(List.of(agentRule));
            var userRuleset = Ruleset.user(List.of(), List.of()).withAskRules(List.of(userRule));
            var engine = new ExecPolicyEngine(List.of(agentRuleset, userRuleset), List.of(), List.of());
            var decision = engine.check(ctx("cargo test --release"));
            assertEquals("tool=exec_shell command=cargo", decision.matchedRule());
        }

        @Test
        void rejectRulesModeStillForbidsMatchingAskRule() {
            var ruleset = Ruleset.agent(List.of(), List.of()).withAskRules(
                List.of(ToolAskRule.execShell("cargo test")));
            var engine = new ExecPolicyEngine(List.of(ruleset), List.of(), List.of());
            var decision = engine.check(ctx("cargo test",
                new AskForApproval.Reject(false, true, false)));
            assertFalse(decision.allow());
            assertInstanceOf(ExecApprovalRequirement.Forbidden.class, decision.requirement());
        }

        @Test
        void typedAskPathMatchingTrimsSpacesBeforeBoundarySlashes() {
            var ruleset = Ruleset.agent(List.of(), List.of()).withAskRules(
                List.of(ToolAskRule.filePath("edit_file", "/tmp/project")));
            var engine = new ExecPolicyEngine(List.of(ruleset), List.of(), List.of());
            var ctx = new ExecPolicyContext("", "/workspace", "edit_file", "  /tmp/project/  ",
                new AskForApproval.UnlessTrusted(), null);
            var decision = engine.check(ctx);
            assertTrue(decision.allow());
            assertTrue(decision.requiresApproval());
            assertEquals("tool=edit_file path=/tmp/project", decision.matchedRule());
        }

        // ── Session approval ────────────────────────────────────────

        @Test
        void rememberSessionApprovalCachesKey() {
            var engine = new ExecPolicyEngine(List.of(), List.of());
            assertFalse(engine.isSessionApproved("git status"));
            engine.rememberSessionApproval("git status");
            assertTrue(engine.isSessionApproved("git status"));
        }

        // ── RulesetLayer ordering ────────────────────────────────────

        @Test
        void rulesetLayerPriorityOrder() {
            assertTrue(RulesetLayer.User.priority() > RulesetLayer.Agent.priority());
            assertTrue(RulesetLayer.Agent.priority() > RulesetLayer.BuiltinDefault.priority());
        }

        // ── Deny word-boundary matching ───────────────────────────────

        @Test
        void denyWordBoundaryDoesNotBlockPrefixOverlap() {
            var engine = new ExecPolicyEngine(List.of(), List.of("rm"));
            // "rmdir" should NOT be blocked by deny of "rm" (word-boundary match)
            var decision = engine.check(ctx("rmdir old-project"));
            assertTrue(decision.allow());
            assertTrue(decision.requiresApproval()); // untrusted, so needs approval
            assertInstanceOf(ExecApprovalRequirement.NeedsApproval.class, decision.requirement());
        }

        @Test
        void denyMatchesExactWord() {
            var engine = new ExecPolicyEngine(List.of(), List.of("rm"));
            var decision = engine.check(ctx("rm -rf /"));
            assertFalse(decision.allow());
            assertInstanceOf(ExecApprovalRequirement.Forbidden.class, decision.requirement());
        }
    }
}
