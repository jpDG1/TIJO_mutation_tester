package mutationtester.execution;

import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.util.List;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.EngineFilter.includeEngines;

public final class TestRunner {

    public TestRunResult runTests(Class<?> testClass) {
        Thread thread = Thread.currentThread();
        ClassLoader previousLoader = thread.getContextClassLoader();
        thread.setContextClassLoader(testClass.getClassLoader());

        try (LauncherSession session = LauncherFactory.openSession()) {
            LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                    .selectors(selectClass(testClass))
                    .filters(includeEngines("junit-jupiter"))
                    .build();
            SummaryGeneratingListener listener = new SummaryGeneratingListener();
            session.getLauncher().execute(request, listener);
            TestExecutionSummary summary = listener.getSummary();
            List<String> failures = summary.getFailures().stream()
                    .map(failure -> failure.getTestIdentifier().getDisplayName() + ": " + failure.getException())
                    .toList();
            return new TestRunResult(summary.getTestsFoundCount(), summary.getTestsSucceededCount(),
                    summary.getTestsFailedCount(), failures);
        } finally {
            thread.setContextClassLoader(previousLoader);
        }
    }
}
