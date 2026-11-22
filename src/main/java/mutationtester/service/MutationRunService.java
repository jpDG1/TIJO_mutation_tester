package mutationtester.service;

import jakarta.annotation.PreDestroy;
import mutationtester.execution.ExecutionResult;
import mutationtester.execution.MutantExecutor;
import mutationtester.execution.MutantStatus;
import mutationtester.execution.TestRunResult;
import mutationtester.mutation.Mutant;
import mutationtester.mutation.MutationGenerator;
import mutationtester.parser.JavaSourceParser;
import mutationtester.progress.ProgressTracker;
import mutationtester.progress.RunProgress;
import mutationtester.report.MutantResult;
import mutationtester.report.MutationReport;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

@Service
public final class MutationRunService {

    private static final Duration EXECUTION_TIMEOUT = Duration.ofSeconds(10);

    private final JavaSourceParser parser = new JavaSourceParser();
    private final MutantExecutor executor = new MutantExecutor();
    private final RunRepository runs = new RunRepository();
    private final ExecutorService jobs = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "mutation-analysis");
        thread.setDaemon(true);
        return thread;
    });
    private final MutationGenerator generator;

    public MutationRunService(MutationGenerator generator) {
        this.generator = generator;
    }

    public UUID startRun(String sourceCode, String testCode) {
        AnalysisInput input = AnalysisInput.parse(parser, sourceCode, testCode);
        validateBaseline(input);

        UUID runId = UUID.randomUUID();
        runs.save(new MutationRun(runId, RunStatus.RUNNING, new RunProgress(0, 0), null, null));
        try {
            jobs.execute(() -> analyze(runId, input));
        } catch (RejectedExecutionException exception) {
            runs.remove(runId);
            throw exception;
        }
        return runId;
    }

    public Optional<MutationRun> findRun(UUID runId) {
        return runs.find(runId);
    }

    @PreDestroy
    void stop() {
        jobs.shutdownNow();
    }

    private void validateBaseline(AnalysisInput input) {
        ExecutionResult result = executor.execute(
                input.compilationSources(input.originalSource()), input.testClassName(), EXECUTION_TIMEOUT);
        if (result.status() == MutantStatus.INVALID) {
            throw new AnalysisRejectedException("The original class or tests did not compile.", result.diagnostics());
        }
        if (result.status() == MutantStatus.TIMED_OUT) {
            throw new AnalysisRejectedException("The original tests exceeded the time limit.", result.diagnostics());
        }
        TestRunResult tests = result.testRun();
        if (tests.testsFound() == 0) {
            throw new AnalysisRejectedException("JUnit did not discover any tests in the test class.");
        }
        if (!tests.allTestsPassed()) {
            throw new AnalysisRejectedException("All tests must pass on the original class.", tests.failures());
        }
    }

    private void analyze(UUID runId, AnalysisInput input) {
        try {
            List<Mutant> mutants = generator.generateMutants(input.sourceTree());
            ProgressTracker progress = new ProgressTracker(mutants.size(), List.of(update ->
                    runs.save(new MutationRun(runId, RunStatus.RUNNING, update, null, null))));
            progress.publishProgress();

            List<MutantResult> results = runMutants(input, mutants, progress);
            MutationReport report = MutationReport.create(input.originalSource(), results);
            runs.save(new MutationRun(runId, RunStatus.COMPLETED, progress.currentProgress(), report, null));
        } catch (RuntimeException exception) {
            RunProgress lastProgress = runs.find(runId).orElseThrow().progress();
            String message = exception.getMessage() == null ? "The analysis failed." : exception.getMessage();
            runs.save(new MutationRun(runId, RunStatus.FAILED, lastProgress, null, message));
        }
    }

    private List<MutantResult> runMutants(AnalysisInput input, List<Mutant> mutants, ProgressTracker progress) {
        List<MutantResult> results = new ArrayList<>(mutants.size());
        for (Mutant mutant : mutants) {
            ExecutionResult result = executor.execute(
                    input.compilationSources(mutant.sourceCode()), input.testClassName(), EXECUTION_TIMEOUT);
            results.add(MutantResult.from(results.size() + 1, mutant, result.status()));
            progress.mutantProcessed();
        }
        return results;
    }
}
