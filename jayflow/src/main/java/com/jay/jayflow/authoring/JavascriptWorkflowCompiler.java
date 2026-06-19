package com.jay.jayflow.authoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.jay.jayflow.authoring.js.JsWorkflowSpec;
import com.jay.jayflow.ir.WorkflowSpec;
import com.jay.jayflow.validation.WorkflowNodeValidator;

/**
 * Compiles JS/TS workflow source files without executing JavaScript.
 * Pure lexical analysis: finds workflow({...}) call, extracts balanced JSON object,
 * validates safety (no runtime constructs), deserializes via JsWorkflowSpec to WorkflowSpec.
 * Equivalent to Rust's js_authoring.rs.
 */
public final class JavascriptWorkflowCompiler {

    private static final ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    private static final String[][] BLOCKED = {
        {"import ", "import"}, {"import(", "dynamic import"}, {"require(", "require"},
        {"fetch(", "fetch"}, {"XMLHttpRequest", "XMLHttpRequest"}, {"WebSocket", "WebSocket"},
        {"process.", "process"}, {"Deno.", "Deno"}, {"Bun.", "Bun"},
        {"child_process", "child_process"}, {"exec(", "exec"}, {"spawn(", "spawn"},
        {"open(", "open"}, {"readFile", "readFile"}, {"writeFile", "writeFile"},
        {"async ", "async"}, {"await ", "await"}, {"eval(", "eval"}, {"new Function", "Function"},
    };

    private JavascriptWorkflowCompiler() {}

    public static WorkflowSpec compileJavascript(String identifier, String source)
            throws JavascriptWorkflowCompileException {
        return compileJsLike(source);
    }

    public static WorkflowSpec compileTypescript(String identifier, String source)
            throws JavascriptWorkflowCompileException {
        source = source.replaceAll("\\bsatisfies\\s+\\w+(\\s*\\{[^}]*\\})?\\s*[;)]", "");
        return compileJsLike(source);
    }

    private static WorkflowSpec compileJsLike(String source) throws JavascriptWorkflowCompileException {
        rejectUnsupportedConstructs(source);
        var json = extractWorkflowObject(source);
        JsWorkflowSpec authored;
        try { authored = mapper.readValue(json, JsWorkflowSpec.class); }
        catch (Exception e) { throw new JavascriptWorkflowCompileException(new JavascriptWorkflowError.InvalidJson(e.getMessage())); }
        var workflow = authored.toWorkflow();
        if (workflow.goal() == null || workflow.goal().trim().isEmpty())
            throw new JavascriptWorkflowCompileException(new JavascriptWorkflowError.InvalidNode("workflow goal cannot be empty"));
        try { WorkflowNodeValidator.validate(workflow.nodes()); }
        catch (Exception e) { throw new JavascriptWorkflowCompileException(new JavascriptWorkflowError.InvalidNode(e.getMessage())); }
        return workflow;
    }

    // ── Safety scanning ─────────────────────────────────────────

    static void rejectUnsupportedConstructs(String source) throws JavascriptWorkflowCompileException {
        for (var pair : BLOCKED) {
            if (source.contains(pair[0]))
                throw new JavascriptWorkflowCompileException(new JavascriptWorkflowError.UnsupportedConstruct(pair[1]));
        }
    }

    // ── Brace-balanced JSON extraction ──────────────────────────

    static String extractWorkflowObject(String source) throws JavascriptWorkflowCompileException {
        int wfPos = source.indexOf("workflow");
        if (wfPos < 0) throw new JavascriptWorkflowCompileException(new JavascriptWorkflowError.MissingWorkflowCall());
        int parenRel = source.indexOf('(', wfPos);
        if (parenRel < 0) throw new JavascriptWorkflowCompileException(new JavascriptWorkflowError.MissingWorkflowCall());
        int openParen = parenRel;

        int objStart = -1;
        for (int i = openParen + 1; i < source.length(); i++) {
            if (!Character.isWhitespace(source.charAt(i))) { objStart = i; break; }
        }
        if (objStart < 0 || source.charAt(objStart) != '{')
            throw new JavascriptWorkflowCompileException(new JavascriptWorkflowError.InvalidWorkflowObject("must receive a JSON-compatible object literal"));

        int depth = 0;
        Character inString = null;
        boolean escape = false;
        for (int i = objStart; i < source.length(); i++) {
            char ch = source.charAt(i);
            if (inString != null) {
                if (escape) escape = false;
                else if (ch == '\\') escape = true;
                else if (ch == inString) inString = null;
                continue;
            }
            if (ch == '"' || ch == '\'' || ch == '`') { inString = ch; continue; }
            if (ch == '{') depth++;
            else if (ch == '}') {
                depth--;
                if (depth < 0) throw new JavascriptWorkflowCompileException(new JavascriptWorkflowError.InvalidWorkflowObject("unbalanced closing brace"));
                if (depth == 0) return source.substring(objStart, i + 1);
            }
        }
        throw new JavascriptWorkflowCompileException(new JavascriptWorkflowError.InvalidWorkflowObject("missing closing brace"));
    }

    public static class JavascriptWorkflowCompileException extends Exception {
        private final JavascriptWorkflowError error;
        public JavascriptWorkflowCompileException(JavascriptWorkflowError error) { super(error.toString()); this.error = error; }
        public JavascriptWorkflowError error() { return error; }
    }
}
