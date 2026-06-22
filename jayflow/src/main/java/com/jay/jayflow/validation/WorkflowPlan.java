package com.jay.jayflow.validation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.jay.jayflow.ir.FailurePolicy;
import com.jay.jayflow.ir.TaskMode;
import com.jay.jayflow.ir.WorkflowConfig;

/** Compiled and validated workflow plan. Equivalent to Rust's WorkflowPlan. */
public record WorkflowPlan(String goal, int maxConcurrent, List<PhasePlan> phases) {

    public record PhasePlan(String name, boolean parallel, FailurePolicy onFailure,
                             List<WorkflowConfig.Task> tasks) { }

    public static WorkflowPlan fromConfig(WorkflowConfig config) {
        var goal = config.goal();
        if (goal == null || goal.isBlank())
            new WorkflowValidationError.EmptyField("workflow goal").throwUnchecked();
        int mc = config.maxConcurrent();
        if (mc < 1 || mc > 20)
            new WorkflowValidationError.InvalidMaxConcurrent(mc).throwUnchecked();
        var phases = config.phases();
        if (phases.isEmpty()) new WorkflowValidationError.EmptyWorkflow().throwUnchecked();

        var phaseIndex = new LinkedHashMap<String, Integer>();
        var allTasks = new HashMap<String, WorkflowConfig.Task>();
        var taskPhase = new HashMap<String, String>();

        for (int i = 0; i < phases.size(); i++) {
            var phase = phases.get(i);
            var name = phase.name();
            if (name == null || name.isBlank())
                new WorkflowValidationError.EmptyField("phase name").throwUnchecked();
            if (phase.tasks().isEmpty())
                new WorkflowValidationError.EmptyPhase(name).throwUnchecked();
            if (phaseIndex.containsKey(name))
                new WorkflowValidationError.DuplicatePhase(name).throwUnchecked();
            phaseIndex.put(name, i);
            for (var task : phase.tasks()) {
                if (task.id() == null || task.id().isBlank())
                    new WorkflowValidationError.EmptyField("task id").throwUnchecked();
                if (task.prompt() == null || task.prompt().isBlank())
                    new WorkflowValidationError.EmptyField("task prompt").throwUnchecked();
                if (allTasks.containsKey(task.id()))
                    new WorkflowValidationError.DuplicateTask(task.id()).throwUnchecked();
                allTasks.put(task.id(), task);
                taskPhase.put(task.id(), name);
            }
        }

        var orderedPhases = topologicalSort(phases, phaseIndex);
        var phaseOrder = new HashMap<String, Integer>();
        for (int i = 0; i < orderedPhases.size(); i++)
            phaseOrder.put(orderedPhases.get(i).name(), i);

        for (var phase : phases) {
            // Validate parallel write scope
            if (phase.parallel()) {
                var writeTasks = phase.tasks().stream()
                        .filter(t -> t.mode() == TaskMode.READ_WRITE).toList();
                for (var t : writeTasks) {
                    if (t.fileScope() == null || t.fileScope().isEmpty())
                        new WorkflowValidationError.MissingParallelWriteScope(t.id()).throwUnchecked();
                }
                for (int i = 0; i < writeTasks.size(); i++) {
                    for (int j = i + 1; j < writeTasks.size(); j++) {
                        var a = writeTasks.get(i).fileScope();
                        var b = writeTasks.get(j).fileScope();
                        if (a != null && b != null && scopesOverlap(a, b))
                            new WorkflowValidationError.OverlappingParallelWriteScope(
                                    writeTasks.get(i).id(), writeTasks.get(j).id()).throwUnchecked();
                    }
                }
            }
            // Validate task result dependencies
            for (var task : phase.tasks()) {
                var deps = task.dependsOnResults() != null ? task.dependsOnResults() : List.<String>of();
                for (var dep : deps) {
                    var depPhase = taskPhase.get(dep);
                    if (depPhase == null)
                        new WorkflowValidationError.InvalidTaskResultDependency(task.id(), dep).throwUnchecked();
                    if (phaseOrder.get(depPhase) >= phaseOrder.get(phase.name()))
                        new WorkflowValidationError.UnavailableTaskResultDependency(
                                task.id(), dep, depPhase, phase.name()).throwUnchecked();
                }
            }
        }

        var planPhases = new ArrayList<PhasePlan>();
        for (var phase : orderedPhases)
            planPhases.add(new PhasePlan(phase.name(), phase.parallel(),
                    phase.onFailure() != null ? phase.onFailure() : FailurePolicy.SKIP_CONTINUE,
                    phase.tasks()));
        return new WorkflowPlan(goal, mc, List.copyOf(planPhases));
    }

    private static List<WorkflowConfig.Phase> topologicalSort(
            List<WorkflowConfig.Phase> phases, Map<String, Integer> phaseIndex) {
        var adj = new HashMap<String, List<String>>();
        var inDeg = new HashMap<String, Integer>();
        for (var p : phases) { adj.putIfAbsent(p.name(), new ArrayList<>()); inDeg.putIfAbsent(p.name(), 0); }
        for (var p : phases) {
            for (var dep : p.dependsOn() != null ? p.dependsOn() : List.<String>of()) {
                if (!phaseIndex.containsKey(dep))
                    new WorkflowValidationError.InvalidPhaseDependency(p.name(), dep).throwUnchecked();
                adj.computeIfAbsent(dep, k -> new ArrayList<>()).add(p.name());
                inDeg.merge(p.name(), 1, Integer::sum);
            }
        }
        var queue = new ArrayDeque<String>();
        for (var p : phases) if (inDeg.get(p.name()) == 0) queue.add(p.name());
        var nameMap = new HashMap<String, WorkflowConfig.Phase>();
        for (var p : phases) nameMap.put(p.name(), p);
        var sorted = new ArrayList<WorkflowConfig.Phase>();
        while (!queue.isEmpty()) {
            var name = queue.poll(); sorted.add(nameMap.get(name));
            for (var next : adj.getOrDefault(name, List.of())) {
                inDeg.merge(next, -1, Integer::sum);
                if (inDeg.get(next) == 0) queue.add(next);
            }
        }
        if (sorted.size() != phases.size())
            new WorkflowValidationError.PhaseDependencyCycle("cycle").throwUnchecked();
        return sorted;
    }

    private static boolean scopesOverlap(List<String> a, List<String> b) {
        for (var sa : a) for (var sb : b)
            if (sa.equals(sb) || sa.startsWith(sb) || sb.startsWith(sa)) return true;
        return false;
    }
}
