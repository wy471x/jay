package com.jay.execpolicy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Arity-aware command prefix classifier.
 *
 * Maps command tokens to canonical prefixes so that policy rules like
 * {@code "git status"} correctly match {@code git status -s} and
 * {@code git status --porcelain} but do NOT match {@code git push}.
 * Flags starting with {@code -} are stripped before matching.
 */
public class BashArityDict {

    private static final String[][] BASH_ARITY_TABLE = {
        // git subcommands (arity 2)
        {"git add", "2"}, {"git am", "2"}, {"git apply", "2"}, {"git archive", "2"},
        {"git bisect", "2"}, {"git blame", "2"}, {"git branch", "2"}, {"git bundle", "2"},
        {"git checkout", "2"}, {"git cherry", "2"}, {"git cherry-pick", "2"}, {"git citool", "2"},
        {"git clean", "2"}, {"git clone", "2"}, {"git commit", "2"}, {"git config", "2"},
        {"git describe", "2"}, {"git diff", "2"}, {"git difftool", "2"}, {"git fetch", "2"},
        {"git format-patch", "2"}, {"git fsck", "2"}, {"git gc", "2"}, {"git grep", "2"},
        {"git gui", "2"}, {"git init", "2"}, {"git log", "2"}, {"git merge", "2"},
        {"git mergetool", "2"}, {"git mv", "2"}, {"git notes", "2"}, {"git pull", "2"},
        {"git push", "2"}, {"git rebase", "2"}, {"git reflog", "2"}, {"git remote", "2"},
        {"git repack", "2"}, {"git reset", "2"}, {"git restore", "2"}, {"git revert", "2"},
        {"git rm", "2"}, {"git shortlog", "2"}, {"git show", "2"}, {"git stash", "2"},
        {"git status", "2"}, {"git submodule", "2"}, {"git switch", "2"}, {"git tag", "2"},
        {"git worktree", "2"},

        // npm subcommands (mostly arity 2, "run" is arity 3)
        {"npm install", "2"}, {"npm uninstall", "2"}, {"npm update", "2"}, {"npm test", "2"},
        {"npm start", "2"}, {"npm stop", "2"}, {"npm restart", "2"}, {"npm publish", "2"},
        {"npm init", "2"}, {"npm version", "2"}, {"npm search", "2"}, {"npm link", "2"},
        {"npm unlink", "2"}, {"npm audit", "2"}, {"npm fund", "2"}, {"npm run", "3"},

        // yarn subcommands
        {"yarn add", "2"}, {"yarn remove", "2"}, {"yarn upgrade", "2"}, {"yarn install", "2"},
        {"yarn test", "2"}, {"yarn build", "2"}, {"yarn start", "2"}, {"yarn run", "3"},
        {"yarn workspace", "3"},

        // pnpm subcommands
        {"pnpm install", "2"}, {"pnpm add", "2"}, {"pnpm remove", "2"}, {"pnpm update", "2"},
        {"pnpm test", "2"}, {"pnpm run", "3"}, {"pnpm exec", "2"},

        // cargo subcommands (all arity 2)
        {"cargo build", "2"}, {"cargo check", "2"}, {"cargo clean", "2"}, {"cargo doc", "2"},
        {"cargo fix", "2"}, {"cargo fmt", "2"}, {"cargo init", "2"}, {"cargo install", "2"},
        {"cargo new", "2"}, {"cargo publish", "2"}, {"cargo run", "2"}, {"cargo test", "2"},
        {"cargo bench", "2"}, {"cargo update", "2"}, {"cargo search", "2"}, {"cargo login", "2"},
        {"cargo logout", "2"}, {"cargo owner", "2"}, {"cargo package", "2"}, {"cargo vendor", "2"},
        {"cargo clippy", "2"},

        // docker subcommands
        {"docker build", "2"}, {"docker exec", "2"}, {"docker images", "2"}, {"docker inspect", "2"},
        {"docker kill", "2"}, {"docker login", "2"}, {"docker logout", "2"}, {"docker logs", "2"},
        {"docker ps", "2"}, {"docker pull", "2"}, {"docker push", "2"}, {"docker restart", "2"},
        {"docker rm", "2"}, {"docker rmi", "2"}, {"docker run", "2"}, {"docker start", "2"},
        {"docker stop", "2"}, {"docker compose", "3"}, {"docker container", "3"},
        {"docker image", "3"}, {"docker network", "3"}, {"docker system", "3"},
        {"docker volume", "3"},

        // kubectl subcommands
        {"kubectl apply", "2"}, {"kubectl config", "2"}, {"kubectl cluster-info", "2"},
        {"kubectl cordon", "2"}, {"kubectl drain", "2"}, {"kubectl exec", "2"},
        {"kubectl explain", "2"}, {"kubectl expose", "2"}, {"kubectl label", "2"},
        {"kubectl logs", "2"}, {"kubectl create", "3"}, {"kubectl delete", "3"},
        {"kubectl describe", "3"}, {"kubectl get", "3"}, {"kubectl rollout", "3"},
        {"kubectl top", "3"},

        // go subcommands
        {"go build", "2"}, {"go doc", "2"}, {"go fmt", "2"}, {"go generate", "2"},
        {"go install", "2"}, {"go run", "2"}, {"go test", "2"}, {"go vet", "2"},
        {"go env", "2"}, {"go get", "2"}, {"go mod", "3"}, {"go work", "3"},

        // python / pip
        {"python", "1"}, {"python3", "1"}, {"python -m", "3"}, {"python3 -m", "3"},
        {"pip install", "2"}, {"pip uninstall", "2"}, {"pip list", "2"}, {"pip freeze", "2"},

        // make / cmake (arity 1: only the base command)
        {"make", "1"}, {"cmake", "1"},

        // gh (GitHub CLI) — all arity 3
        {"gh issue", "3"}, {"gh pr", "3"}, {"gh release", "3"}, {"gh repo", "3"},
        {"gh run", "3"}, {"gh secret", "3"}, {"gh workflow", "3"},

        // rustup
        {"rustup component", "2"}, {"rustup default", "2"}, {"rustup install", "2"},
        {"rustup show", "2"}, {"rustup target", "3"}, {"rustup toolchain", "3"},

        // deno / bun / npx
        {"deno run", "2"}, {"deno test", "2"}, {"deno fmt", "2"}, {"deno lint", "2"},
        {"deno compile", "2"}, {"bun run", "3"}, {"bun install", "2"}, {"bun test", "2"},
        {"npx", "1"},

        // aws CLI
        {"aws s3", "3"}, {"aws ec2", "3"}, {"aws lambda", "3"}, {"aws iam", "3"},
        {"aws dynamodb", "3"}, {"aws cloudformation", "3"}, {"aws configure", "2"},
        {"aws ecr", "3"}, {"aws ecs", "3"}, {"aws rds", "3"},

        // terraform
        {"terraform apply", "2"}, {"terraform destroy", "2"}, {"terraform fmt", "2"},
        {"terraform init", "2"}, {"terraform plan", "2"}, {"terraform validate", "2"},
        {"terraform state", "3"}, {"terraform workspace", "3"},

        // helm
        {"helm install", "2"}, {"helm upgrade", "2"}, {"helm uninstall", "2"},
        {"helm list", "2"}, {"helm status", "2"}, {"helm template", "2"},
        {"helm lint", "2"}, {"helm repo", "3"},

        // Additional common tools
        {"mvn", "1"}, {"gradle", "1"}, {"gradlew", "1"}, {"java", "1"}, {"javac", "1"},
        {"kotlin", "1"}, {"scala", "1"}, {"clang", "1"}, {"gcc", "1"}, {"g++", "1"},
        {"lldb", "1"}, {"gdb", "1"}, {"perl", "1"}, {"ruby", "1"}, {"php", "1"},
        {"node", "1"}, {"tsc", "1"}, {"eslint", "1"}, {"prettier", "1"},
    };

    private final Map<String, Integer> entries; // canonical prefix -> arity

    public BashArityDict() {
        entries = new LinkedHashMap<>();
        for (String[] entry : BASH_ARITY_TABLE) {
            entries.put(entry[0], Integer.parseInt(entry[1]));
        }
        // Sort by prefix length descending for greedy longest-match
        var sorted = new LinkedHashMap<String, Integer>();
        entries.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()))
            .forEach(e -> sorted.put(e.getKey(), e.getValue()));
        entries.clear();
        entries.putAll(sorted);
    }

    /**
     * Classify shell command tokens into a canonical prefix by stripping flags
     * and consulting the arity dictionary.
     */
    public String classify(List<String> tokens) {
        if (tokens.isEmpty()) return "";

        // Strip all tokens starting with '-'
        List<String> positional = new ArrayList<>();
        for (String t : tokens) {
            if (!t.startsWith("-")) positional.add(t.toLowerCase());
        }
        if (positional.isEmpty()) return "";

        // Try candidates of depth 1-3, longest first
        for (int depth = Math.min(3, positional.size()); depth >= 1; depth--) {
            var sb = new StringBuilder();
            for (int i = 0; i < depth; i++) {
                if (i > 0) sb.append(' ');
                sb.append(positional.get(i));
            }
            String candidate = sb.toString();
            Integer arity = entries.get(candidate);
            if (arity != null) {
                // Return first 'arity' positional tokens joined by space
                var result = new StringBuilder();
                for (int i = 0; i < Math.min(arity, positional.size()); i++) {
                    if (i > 0) result.append(' ');
                    result.append(positional.get(i));
                }
                return result.toString();
            }
        }

        // Fallback: return just the first positional token (base command)
        return positional.get(0);
    }

    /**
     * Check whether a command matches an allow rule pattern using arity-aware
     * classification.
     */
    public boolean allowRuleMatches(String pattern, String command) {
        if (command == null || command.isBlank()) return false;
        String normalized = normalizeCommand(command);
        String[] tokens = normalized.split(" ");
        String classified = classify(Arrays.asList(tokens));

        String pat = pattern.toLowerCase().trim();
        if (pat.isEmpty()) return false;

        // Primary check: exact match of classified prefix against pattern
        if (classified.equals(pat)) return true;

        // Fallback for patterns not in the arity table: word-boundary prefix match
        if (!entries.containsKey(pat)) {
            return normalized.equals(pat) || normalized.startsWith(pat + " ");
        }
        return false;
    }

    public int size() {
        return entries.size();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public Set<Map.Entry<String, Integer>> entrySet() {
        return Collections.unmodifiableSet(entries.entrySet());
    }

    /** Lowercase, collapse whitespace. */
    static String normalizeCommand(String value) {
        return value.toLowerCase().replaceAll("\\s+", " ").trim();
    }

    /** Return the first whitespace-delimited token of a command. */
    static String firstToken(String command) {
        String trimmed = command.trim();
        int space = trimmed.indexOf(' ');
        return space >= 0 ? trimmed.substring(0, space) : trimmed;
    }

    /** Normalize a path value for comparison. */
    static String normalizePathValue(String value) {
        if (value == null) return "";
        return value.replace('\\', '/')
            .trim()
            .replaceAll("^/+|/+$", "")
            .toLowerCase();
    }
}
