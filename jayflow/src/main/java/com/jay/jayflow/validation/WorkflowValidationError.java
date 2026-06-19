package com.jay.jayflow.validation;

public sealed interface WorkflowValidationError
        permits WorkflowValidationError.EmptyField, WorkflowValidationError.EmptyWorkflow,
               WorkflowValidationError.EmptyPhase, WorkflowValidationError.InvalidMaxConcurrent,
               WorkflowValidationError.DuplicatePhase, WorkflowValidationError.DuplicateTask,
               WorkflowValidationError.InvalidPhaseDependency, WorkflowValidationError.PhaseDependencyCycle,
               WorkflowValidationError.InvalidTaskResultDependency, WorkflowValidationError.UnavailableTaskResultDependency,
               WorkflowValidationError.MissingParallelWriteScope, WorkflowValidationError.OverlappingParallelWriteScope {

    record EmptyField(String field) implements WorkflowValidationError {}
    record EmptyWorkflow() implements WorkflowValidationError {}
    record EmptyPhase(String phase) implements WorkflowValidationError {}
    record InvalidMaxConcurrent(int value) implements WorkflowValidationError {}
    record DuplicatePhase(String phase) implements WorkflowValidationError {}
    record DuplicateTask(String task) implements WorkflowValidationError {}
    record InvalidPhaseDependency(String phase, String dependency) implements WorkflowValidationError {}
    record PhaseDependencyCycle(String phase) implements WorkflowValidationError {}
    record InvalidTaskResultDependency(String task, String dependency) implements WorkflowValidationError {}
    record UnavailableTaskResultDependency(String task, String dependency,
                                            String dependencyPhase, String taskPhase) implements WorkflowValidationError {}
    record MissingParallelWriteScope(String task) implements WorkflowValidationError {}
    record OverlappingParallelWriteScope(String left, String right) implements WorkflowValidationError {}

    default void throwUnchecked() { throw new Wrapped(this); }

    final class Wrapped extends RuntimeException {
        private final WorkflowValidationError error;
        Wrapped(WorkflowValidationError e) { super(e.toString()); this.error = e; }
        public WorkflowValidationError error() { return error; }
    }
}
