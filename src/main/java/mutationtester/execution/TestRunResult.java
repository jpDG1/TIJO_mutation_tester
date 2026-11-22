package mutationtester.execution;

import java.util.List;

public record TestRunResult(long testsFound, long testsSucceeded, long testsFailed, List<String> failures) {

    public TestRunResult {
        failures = List.copyOf(failures);
    }

    public boolean allTestsPassed() {
        return testsFound > 0 && testsSucceeded == testsFound && failures.isEmpty();
    }
}
