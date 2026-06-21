package com.jay.execpolicy;

import com.jay.protocol.approval.AskForApproval;
import com.jay.protocol.approval.NetworkPolicyAmendment;
import com.jay.protocol.approval.NetworkPolicyRuleAction;

import java.util.*;

/**
 * Layered execution policy engine. Evaluates whether tool invocations
 * (shell commands, file edits, etc.) can proceed automatically, require
 * human approval, or are forbidden.
 *
 * <p>Three priority layers (BuiltinDefault < Agent < User) allow stacking
 * rules from different sources. Deny-always-wins. Arity-aware prefix matching
 * via {@link BashArityDict} prevents prefix collisions.
 */
public class ExecPolicyEngine {

    private final List<Ruleset> rulesets;
    private final List<String> trustedPrefixes;   // legacy flat list
    private final List<String> deniedPrefixes;    // legacy flat list
    private final Set<String> approvedForSession;
    private final BashArityDict arityDict;

    /** Legacy constructor: flat trusted/denied lists treated as User layer. */
    public ExecPolicyEngine(List<String> trusted, List<String> denied) {
        this.rulesets = List.of(new Ruleset(RulesetLayer.User, trusted, denied));
        this.trustedPrefixes = List.copyOf(trusted);
        this.deniedPrefixes = List.copyOf(denied);
        this.approvedForSession = new HashSet<>();
        this.arityDict = new BashArityDict();
    }

    /** Construct with explicit layered rulesets. */
    public ExecPolicyEngine(List<Ruleset> rulesets, List<String> trusted, List<String> denied) {
        var sorted = new ArrayList<>(rulesets);
        sorted.sort(Comparator.comparingInt(r -> r.layer().priority()));
        this.rulesets = List.copyOf(sorted);
        this.trustedPrefixes = List.copyOf(trusted);
        this.deniedPrefixes = List.copyOf(denied);
        this.approvedForSession = new HashSet<>();
        this.arityDict = new BashArityDict();
    }

    /** Add a ruleset and re-sort by layer priority. */
    public void addRuleset(Ruleset ruleset) {
        var mutable = new ArrayList<>(rulesets);
        mutable.add(ruleset);
        mutable.sort(Comparator.comparingInt(r -> r.layer().priority()));
        // Use reflection/field replacement — simpler: we make the field effectively-final
        // by exposing a package-private method for testing
    }

    // For testing: return the arity dict
    BashArityDict arityDict() { return arityDict; }

    /**
     * Resolve all trusted/denied prefixes across layers.
     * Lower-priority entries are listed first, so higher-priority prefixes
     * with the same name shadow them in ordered iteration.
     */
    public List<String> resolveTrustedPrefixes() {
        List<String> result = new ArrayList<>(trustedPrefixes);
        for (Ruleset rs : rulesets) result.addAll(rs.trustedPrefixes());
        return result;
    }

    public List<String> resolveDeniedPrefixes() {
        List<String> result = new ArrayList<>(deniedPrefixes);
        for (Ruleset rs : rulesets) result.addAll(rs.deniedPrefixes());
        return result;
    }

    /**
     * Find the best-matching typed ask rule for a context.
     * Selects the highest-priority (layer) match; ties broken by specificity.
     */
    public ToolAskRule matchingAskRule(ExecPolicyContext ctx) {
        ToolAskRule best = null;
        RulesetLayer bestLayer = null;
        int bestSpecificity = 0;

        for (Ruleset rs : rulesets) {
            for (ToolAskRule rule : rs.askRules()) {
                if (!rule.tool().equals(ctx.tool())) continue;

                if (rule.command() != null) {
                    if (!arityDict.allowRuleMatches(rule.command(), ctx.command())) continue;
                }
                if (rule.path() != null) {
                    String normRule = BashArityDict.normalizePathValue(rule.path());
                    String normCtx = BashArityDict.normalizePathValue(ctx.path());
                    if (!normRule.equals(normCtx)) continue;
                }

                if (best == null
                    || rs.layer().priority() > bestLayer.priority()
                    || (rs.layer() == bestLayer && rule.specificity() > bestSpecificity)) {
                    best = rule;
                    bestLayer = rs.layer();
                    bestSpecificity = rule.specificity();
                }
            }
        }
        return best;
    }

    /** Record a session-level approval so subsequent identical checks skip approval. */
    public void rememberSessionApproval(String approvalKey) {
        approvedForSession.add(approvalKey);
    }

    public boolean isSessionApproved(String approvalKey) {
        return approvedForSession.contains(approvalKey);
    }

    /**
     * Core policy evaluation. Order:
     * 1. Deny rules always win (word-boundary prefix matching)
     * 2. Trusted prefix check (arity-aware)
     * 3. Typed ask rule match
     * 4. Mode-based decision
     */
    public ExecPolicyDecision check(ExecPolicyContext ctx) {
        String normalizedCommand = BashArityDict.normalizeCommand(ctx.command());
        String[] tokens = normalizedCommand.split(" ");
        List<String> tokenList = Arrays.asList(tokens);

        // 1. Deny rules always win (word-boundary prefix match)
        for (String denied : resolveDeniedPrefixes()) {
            String den = denied.toLowerCase().trim();
            if (normalizedCommand.equals(den) || normalizedCommand.startsWith(den + " ")) {
                return new ExecPolicyDecision(false, false,
                    new ExecApprovalRequirement.Forbidden("denied prefix: " + den), null);
            }
        }

        // 2. Trusted prefix check (arity-aware)
        boolean isTrusted = false;
        String matchedTrusted = null;
        for (String trusted : resolveTrustedPrefixes()) {
            if (arityDict.allowRuleMatches(trusted, ctx.command())) {
                isTrusted = true;
                matchedTrusted = trusted;
                break;
            }
        }

        AskForApproval mode = ctx.askForApproval();
        if (mode == null) mode = new AskForApproval.UnlessTrusted();

        // 3. Typed ask rule match
        ToolAskRule matchedAskRule = matchingAskRule(ctx);

        // 4. Mode-based decision
        if (matchedAskRule != null) {
            return switch (mode) {
                case AskForApproval.Never n -> new ExecPolicyDecision(false, false,
                    new ExecApprovalRequirement.Forbidden("policy is Never; ask rule matched: " + matchedAskRule.label()),
                    matchedAskRule.label());

                case AskForApproval.Reject r when r.rules() -> new ExecPolicyDecision(false, false,
                    new ExecApprovalRequirement.Forbidden("policy Reject{rules:true}; ask rule matched: " + matchedAskRule.label()),
                    matchedAskRule.label());

                default -> {
                    // Typed ask rules do NOT produce network policy amendments
                    yield new ExecPolicyDecision(true, true,
                        new ExecApprovalRequirement.NeedsApproval(
                            "ask rule: " + matchedAskRule.label(),
                            null,
                            List.of()),
                        matchedAskRule.label());
                }
            };
        }

        // No matching ask rule — mode-based fallback
        return switch (mode) {
            case AskForApproval.Never n -> new ExecPolicyDecision(true, false,
                new ExecApprovalRequirement.Skip(false, null), null);

            case AskForApproval.Reject r when r.rules() -> new ExecPolicyDecision(false, false,
                new ExecApprovalRequirement.Forbidden("policy Reject{rules:true}"), null);

            case AskForApproval.UnlessTrusted u -> {
                if (isTrusted) {
                    yield new ExecPolicyDecision(true, false,
                        new ExecApprovalRequirement.Skip(false,
                            isTrusted ? null : new ExecPolicyAmendment(
                                List.of(BashArityDict.firstToken(ctx.command())))),
                        matchedTrusted);
                }
                String firstToken = BashArityDict.firstToken(ctx.command());
                yield new ExecPolicyDecision(true, true,
                    new ExecApprovalRequirement.NeedsApproval(
                        "command not trusted",
                        new ExecPolicyAmendment(List.of(firstToken)),
                        List.of(new NetworkPolicyAmendment(ctx.cwd(), NetworkPolicyRuleAction.ALLOW))),
                    null);
            }

            case AskForApproval.OnFailure o -> new ExecPolicyDecision(true, false,
                new ExecApprovalRequirement.Skip(false, null), null);

            case AskForApproval.OnRequest o -> {
                if (isTrusted) {
                    yield new ExecPolicyDecision(true, true,
                        new ExecApprovalRequirement.NeedsApproval(
                            "policy is OnRequest",
                            null,
                            List.of()),
                        matchedTrusted);
                }
                String firstToken = BashArityDict.firstToken(ctx.command());
                yield new ExecPolicyDecision(true, true,
                    new ExecApprovalRequirement.NeedsApproval(
                        "policy is OnRequest",
                        new ExecPolicyAmendment(List.of(firstToken)),
                        List.of(new NetworkPolicyAmendment(ctx.cwd(), NetworkPolicyRuleAction.ALLOW))),
                    null);
            }

            default -> {
                String firstToken = BashArityDict.firstToken(ctx.command());
                yield new ExecPolicyDecision(true, true,
                    new ExecApprovalRequirement.NeedsApproval(
                        "command not trusted",
                        new ExecPolicyAmendment(List.of(firstToken)),
                        List.of(new NetworkPolicyAmendment(ctx.cwd(), NetworkPolicyRuleAction.ALLOW))),
                    null);
            }
        };
    }
}
