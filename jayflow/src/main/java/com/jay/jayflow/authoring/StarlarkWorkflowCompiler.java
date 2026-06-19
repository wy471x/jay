package com.jay.jayflow.authoring;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jay.jayflow.authoring.StarlarkWorkflowCompiler.StarlarkCompileException;
import com.jay.jayflow.ir.*;
import com.jay.jayflow.ir.spec.*;
import com.jay.jayflow.validation.WorkflowNodeValidator;

import java.util.*;

/**
 * Compiles Starlark workflow source files using a lightweight recursive-descent
 * parser for the limited Starlark-workflow DSL (no loops, conditionals, or comprehensions).
 * Equivalent to Rust's starlark_authoring.rs.
 */
public final class StarlarkWorkflowCompiler {

    private static final String[][] BLOCKED = {
        {"load(", "load"}, {"import ", "import"}, {"class ", "class"},
        {"while ", "while"}, {"async ", "async"}, {"await ", "await"}, {"open(", "open"},
    };
    private static final ObjectMapper mapper = new ObjectMapper();

    private StarlarkWorkflowCompiler() {}

    // ── Public API ──────────────────────────────────────────────

    public static WorkflowSpec compile(String identifier, String source) {
        rejectUnsupportedConstructs(source);
        var tokens = tokenize(source);
        var parser = new Parser(tokens);
        var result = parser.parseModule();
        if (result == null) throw new StarlarkCompileException(new StarlarkWorkflowError.MissingWorkflow());
        try { WorkflowNodeValidator.validate(result.nodes()); }
        catch (Exception e) { throw new StarlarkCompileException(new StarlarkWorkflowError.InvalidNode(e.getMessage())); }
        return result;
    }

    public static WorkflowSpec compileWithRepair(String identifier, String source) {
        try { return compile(identifier, source); }
        catch (StarlarkCompileException first) {
            var repaired = repairOnce(source);
            if (repaired.equals(source)) throw first;
            return compile(identifier, repaired);
        }
    }

    public static String repairOnce(String source) {
        return source
                .replace("ctx.parallel(", "branch(")
                .replace("ctx.sequence(", "sequence(")
                .replace("ctx.loop_until(", "loop_until(")
                .replace("ctx.when(", "when(")
                .replace("ctx.expand(", "expand(")
                .replace("ctx.tournament(", "tournament(")
                .replace("ctx.teacher.review(", "teacher_review(");
    }

    static void rejectUnsupportedConstructs(String source) {
        for (var pair : BLOCKED)
            if (source.contains(pair[0]))
                throw new StarlarkCompileException(new StarlarkWorkflowError.UnsupportedConstruct(pair[1]));
    }

    public static class StarlarkCompileException extends RuntimeException {
        private final StarlarkWorkflowError error;
        public StarlarkCompileException(StarlarkWorkflowError e) { super(e.toString()); this.error = e; }
        public StarlarkWorkflowError error() { return error; }
    }

    // ── Tokenizer ───────────────────────────────────────────────

    record Token(Kind kind, String value, int pos) { enum Kind { IDENT, STRING, INT, COMMA, LPAREN, RPAREN, LBRACKET, RBRACKET, EQUALS, EOF } }

    private static List<Token> tokenize(String source) {
        var tokens = new ArrayList<Token>();
        int i = 0;
        while (i < source.length()) {
            char ch = source.charAt(i);
            if (Character.isWhitespace(ch) || ch == '\n' || ch == '\r') { i++; continue; }
            if (ch == '#') { while (i < source.length() && source.charAt(i) != '\n') i++; continue; }
            if (ch == '(') { tokens.add(new Token(Token.Kind.LPAREN, "(", i++)); continue; }
            if (ch == ')') { tokens.add(new Token(Token.Kind.RPAREN, ")", i++)); continue; }
            if (ch == '[') { tokens.add(new Token(Token.Kind.LBRACKET, "[", i++)); continue; }
            if (ch == ']') { tokens.add(new Token(Token.Kind.RBRACKET, "]", i++)); continue; }
            if (ch == ',') { tokens.add(new Token(Token.Kind.COMMA, ",", i++)); continue; }
            if (ch == '=') { tokens.add(new Token(Token.Kind.EQUALS, "=", i++)); continue; }
            if (ch == '"' || ch == '\'') {
                var sb = new StringBuilder(); char quote = ch; i++;
                while (i < source.length()) {
                    char c = source.charAt(i++);
                    if (c == '\\') { if (i < source.length()) sb.append(source.charAt(i++)); }
                    else if (c == quote) break;
                    else sb.append(c);
                }
                tokens.add(new Token(Token.Kind.STRING, sb.toString(), i));
                continue;
            }
            if (Character.isLetter(ch) || ch == '_') {
                int start = i;
                while (i < source.length() && (Character.isLetterOrDigit(source.charAt(i)) || source.charAt(i) == '_' || source.charAt(i) == '.')) i++;
                tokens.add(new Token(Token.Kind.IDENT, source.substring(start, i), start));
                continue;
            }
            if (Character.isDigit(ch) || (ch == '-' && i + 1 < source.length() && Character.isDigit(source.charAt(i + 1)))) {
                int start = i;
                if (ch == '-') i++;
                while (i < source.length() && Character.isDigit(source.charAt(i))) i++;
                tokens.add(new Token(Token.Kind.INT, source.substring(start, i), start));
                continue;
            }
            i++;
        }
        tokens.add(new Token(Token.Kind.EOF, "", source.length()));
        return tokens;
    }

    // ── Recursive Descent Parser ────────────────────────────────

    static class Parser {
        final List<Token> tokens;
        int pos;
        Parser(List<Token> tokens) { this.tokens = tokens; }

        Token peek() { return tokens.get(pos); }
        Token eat(Token.Kind kind) { var t = peek(); if (t.kind() != kind) throw new StarlarkCompileException(new StarlarkWorkflowError.Starlark("expected " + kind + " at " + t.pos() + " got " + t.kind())); pos++; return t; }

        WorkflowSpec parseModule() {
            while (pos < tokens.size() - 1) {
                var t = peek();
                if (t.kind() == Token.Kind.IDENT && t.value().equals("workflow")) {
                    return parseWorkflowCall();
                }
                pos++;
            }
            return null;
        }

        WorkflowSpec parseWorkflowCall() {
            eat(Token.Kind.IDENT); // workflow
            eat(Token.Kind.LPAREN);
            String id = null, goal = null, description = null;
            List<WorkflowNode> nodes = List.of();
            while (peek().kind() != Token.Kind.RPAREN && peek().kind() != Token.Kind.EOF) {
                if (peek().kind() == Token.Kind.COMMA) { pos++; continue; }
                if (peek().kind() == Token.Kind.IDENT) {
                    String key = eat(Token.Kind.IDENT).value();
                    eat(Token.Kind.EQUALS);
                    switch (key) {
                        case "id" -> id = parseString();
                        case "goal" -> goal = parseString();
                        case "description" -> description = parseString();
                        case "nodes" -> nodes = parseNodeList();
                        default -> throw new StarlarkCompileException(new StarlarkWorkflowError.Starlark("unknown workflow key: " + key));
                    }
                } else pos++;
            }
            eat(Token.Kind.RPAREN);
            return new WorkflowSpec(id, goal, description, defaultBudget(), defaultPerms(), defaultModel(), defaultPromotion(), nodes);
        }

        List<WorkflowNode> parseNodeList() {
            eat(Token.Kind.LBRACKET);
            var list = new ArrayList<WorkflowNode>();
            while (peek().kind() != Token.Kind.RBRACKET && peek().kind() != Token.Kind.EOF) {
                if (peek().kind() == Token.Kind.COMMA) { pos++; continue; }
                if (peek().kind() == Token.Kind.IDENT) {
                    list.add(parseBuiltinCall());
                } else pos++;
            }
            eat(Token.Kind.RBRACKET);
            return list;
        }

        WorkflowNode parseBuiltinCall() {
            String name = eat(Token.Kind.IDENT).value();
            eat(Token.Kind.LPAREN);
            var kwargs = parseKwargs();
            eat(Token.Kind.RPAREN);
            return evalBuiltin(name, kwargs);
        }

        Map<String, Object> parseKwargs() {
            var map = new LinkedHashMap<String, Object>();
            while (peek().kind() != Token.Kind.RPAREN && peek().kind() != Token.Kind.EOF && peek().kind() != Token.Kind.RBRACKET) {
                if (peek().kind() == Token.Kind.COMMA) { pos++; continue; }
                if (peek().kind() == Token.Kind.IDENT) {
                    String key = eat(Token.Kind.IDENT).value();
                    eat(Token.Kind.EQUALS);
                    Object val = parseValue();
                    map.put(key, val);
                } else pos++;
            }
            return map;
        }

        Object parseValue() {
            var t = peek();
            if (t.kind() == Token.Kind.STRING) return eat(Token.Kind.STRING).value();
            if (t.kind() == Token.Kind.INT) return Long.parseLong(eat(Token.Kind.INT).value());
            if (t.kind() == Token.Kind.IDENT && (t.value().equals("True") || t.value().equals("true"))) { pos++; return true; }
            if (t.kind() == Token.Kind.IDENT && (t.value().equals("False") || t.value().equals("false"))) { pos++; return false; }
            if (t.kind() == Token.Kind.IDENT && t.value().equals("None")) { pos++; return null; }
            if (t.kind() == Token.Kind.LBRACKET) {
                pos++; var list = new ArrayList<>();
                while (peek().kind() != Token.Kind.RBRACKET && peek().kind() != Token.Kind.EOF) {
                    if (peek().kind() == Token.Kind.COMMA) { pos++; continue; }
                    if (peek().kind() == Token.Kind.IDENT) list.add(new WorkflowNode[]{parseBuiltinCall()});
                    else list.add(parseValue());
                }
                eat(Token.Kind.RBRACKET);
                return list;
            }
            if (t.kind() == Token.Kind.IDENT) return parseBuiltinCall();
            throw new StarlarkCompileException(new StarlarkWorkflowError.Starlark("unexpected token at " + t.pos() + ": " + t.kind()));
        }

        String parseString() { return eat(Token.Kind.STRING).value(); }
    }

    // ── Built-in function evaluator ────────────────────────────

    @SuppressWarnings("unchecked")
    static WorkflowNode evalBuiltin(String name, Map<String, Object> kwargs) {
        return switch (name) {
            case "agent" -> agent(kwargs);
            case "test" -> test(kwargs);
            case "search" -> search(kwargs);
            case "shell" -> shell(kwargs);
            case "branch", "ctx.parallel" -> branch(kwargs);
            case "sequence", "ctx.sequence" -> sequence(kwargs);
            case "reduce" -> reduce(kwargs);
            case "teacher_review", "tournament", "ctx.tournament", "ctx.teacher.review" -> teacherReview(kwargs);
            case "loop_until", "ctx.loop_until" -> loopUntil(kwargs);
            case "when", "ctx.when" -> cond(kwargs);
            case "expand", "ctx.expand" -> expand(kwargs);
            default -> throw new StarlarkCompileException(new StarlarkWorkflowError.InvalidNode("unknown builtin: " + name));
        };
    }

    // ── Built-in implementations ───────────────────────────────

    /** Build a LeafSpec from kwargs. Equivalent to Rust's leaf_spec() with shared defaults. */
    public static LeafSpec leafSpec(String id, String prompt, String agentType, String mode,
                                      String isolation, List<String> fileScope, List<String> dependsOnResults) {
        return new LeafSpec(id, prompt,
                agentType(agentType), taskMode(mode), isolationMode(isolation),
                fileScope != null ? fileScope : List.of(),
                dependsOnResults != null ? dependsOnResults : List.of(),
                defaultBudget(), defaultPerms(), defaultModel());
    }

    static WorkflowNode agent(Map<String, Object> kw) {
        return new WorkflowNode.Leaf(leafSpec(
                str(kw, "id"), str(kw, "prompt"),
                optStr(kw, "agent_type"), optStr(kw, "mode"), optStr(kw, "isolation"),
                strings(kw, "file_scope"), strings(kw, "depends_on_results")));
    }

    static WorkflowNode test(Map<String, Object> kw) {
        return agent(kwArgs("id", str(kw, "id"), "prompt", "Run test command: " + str(kw, "command"),
                "agent_type", "verifier", "mode", "read_only", "isolation", "shared",
                "file_scope", kw.get("file_scope")));
    }

    static WorkflowNode search(Map<String, Object> kw) {
        return agent(kwArgs("id", str(kw, "id"), "prompt", "Search codebase: " + str(kw, "query"),
                "agent_type", "explore", "mode", "read_only", "isolation", "shared",
                "file_scope", kw.get("file_scope")));
    }

    static WorkflowNode shell(Map<String, Object> kw) {
        return agent(kwArgs("id", str(kw, "id"), "prompt", "Run shell command: " + str(kw, "command"),
                "agent_type", "verifier", "mode", "read_only", "isolation", "shared",
                "file_scope", kw.get("file_scope")));
    }

    static WorkflowNode branch(Map<String, Object> kw) {
        boolean parallel = !kw.containsKey("parallel") || bool(kw, "parallel");
        return new WorkflowNode.BranchSet(new BranchSpec(
                str(kw, "id"), null, parallel, defaultBudget(), defaultPerms(), defaultModel(), nodeList(kw, "children")));
    }

    static WorkflowNode sequence(Map<String, Object> kw) {
        return new WorkflowNode.Sequence(new SequenceSpec(str(kw, "id"), nodeList(kw, "children")));
    }

    static WorkflowNode reduce(Map<String, Object> kw) {
        return new WorkflowNode.Reduce(new ReduceSpec(str(kw, "id"), strings(kw, "inputs"), str(kw, "prompt"), defaultModel()));
    }

    static WorkflowNode teacherReview(Map<String, Object> kw) {
        return new WorkflowNode.TeacherReview(new TeacherReviewSpec(str(kw, "id"), strings(kw, "candidates"), defaultPromotion()));
    }

    static WorkflowNode loopUntil(Map<String, Object> kw) {
        Integer max = kw.containsKey("max_iterations") ? ((Long) kw.get("max_iterations")).intValue() : null;
        return new WorkflowNode.LoopUntil(new LoopUntilSpec(str(kw, "id"), str(kw, "condition"), max, nodeList(kw, "children")));
    }

    static WorkflowNode cond(Map<String, Object> kw) {
        return new WorkflowNode.Cond(new CondSpec(str(kw, "id"), str(kw, "condition"), nodeList(kw, "then_nodes"), nodeList(kw, "else_nodes")));
    }

    static WorkflowNode expand(Map<String, Object> kw) {
        Integer max = kw.containsKey("max_children") ? ((Long) kw.get("max_children")).intValue() : null;
        return new WorkflowNode.Expand(new ExpandSpec(str(kw, "id"), str(kw, "source"), max, null));
    }

    // ── Enum parsers ───────────────────────────────────────────

    static AgentType agentType(String v) {
        if (v == null) return AgentType.GENERAL;
        return switch (v) {
            case "general" -> AgentType.GENERAL;
            case "explore", "explorer" -> AgentType.EXPLORE;
            case "plan" -> AgentType.PLAN;
            case "review" -> AgentType.REVIEW;
            case "implementer", "implement" -> AgentType.IMPLEMENTER;
            case "verifier", "verify" -> AgentType.VERIFIER;
            default -> throw new StarlarkCompileException(new StarlarkWorkflowError.InvalidEnum("agent_type", v));
        };
    }

    static TaskMode taskMode(String v) {
        if (v == null) return TaskMode.READ_ONLY;
        return switch (v) {
            case "read_only" -> TaskMode.READ_ONLY;
            case "read_write" -> TaskMode.READ_WRITE;
            default -> throw new StarlarkCompileException(new StarlarkWorkflowError.InvalidEnum("mode", v));
        };
    }

    static IsolationMode isolationMode(String v) {
        if (v == null) return IsolationMode.SHARED;
        return switch (v) {
            case "shared" -> IsolationMode.SHARED;
            case "worktree" -> IsolationMode.WORKTREE;
            default -> throw new StarlarkCompileException(new StarlarkWorkflowError.InvalidEnum("isolation", v));
        };
    }

    // ── Helpers ────────────────────────────────────────────────

    static String str(Map<String, Object> kw, String key) { return (String) kw.getOrDefault(key, ""); }
    static String optStr(Map<String, Object> kw, String key) { return (String) kw.get(key); }
    static boolean bool(Map<String, Object> kw, String key) { return (boolean) kw.getOrDefault(key, false); }

    @SuppressWarnings("unchecked")
    static List<WorkflowNode> nodeList(Map<String, Object> kw, String key) {
        var val = kw.get(key);
        if (val == null) return List.of();
        if (val instanceof List<?> list) {
            return list.stream()
                    .filter(item -> item instanceof WorkflowNode[] arr && arr.length == 1)
                    .map(item -> ((WorkflowNode[]) item)[0])
                    .toList();
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    static List<String> strList(Map<String, Object> kw, String key) { return strings(kw, key); }
    static List<String> strings(Map<String, Object> kw, String key) {
        var val = kw.get(key);
        if (val == null) return List.of();
        if (val instanceof List<?> list)
            return list.stream().filter(item -> item instanceof String).map(item -> (String) item).toList();
        return List.of();
    }

    static Map<String, Object> kwArgs(Object... pairs) {
        var m = new LinkedHashMap<String, Object>();
        for (int i = 0; i < pairs.length; i += 2) m.put((String) pairs[i], pairs[i + 1]);
        return m;
    }

    static WorkflowConfig.BudgetSpec defaultBudget() { return new WorkflowConfig.BudgetSpec(null, null, null); }
    static WorkflowConfig.PermissionSpec defaultPerms() { return new WorkflowConfig.PermissionSpec(false, false, List.of(), List.of()); }
    static WorkflowConfig.ModelPolicy defaultModel() { return new WorkflowConfig.ModelPolicy(null, null, List.of()); }
    static WorkflowConfig.PromotionPolicy defaultPromotion() { return new WorkflowConfig.PromotionPolicy(PromotionStrategy.ALL, false, null, new WorkflowConfig.PromotionGateSpec()); }
}
