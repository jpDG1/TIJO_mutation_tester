package mutationtester.execution;

import java.util.List;

public record ExecutionResult(MutantStatus status, TestRunResult testRun, List<String> diagnostics) {

    public ExecutionResult {
        diagnostics = List.copyOf(diagnostics);
    }

    static ExecutionResult withoutTests(MutantStatus status, List<String> diagnostics) {
        return new ExecutionResult(status, new TestRunResult(0, 0, 0, List.of()), diagnostics);
    }
}
