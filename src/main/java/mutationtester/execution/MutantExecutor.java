package mutationtester.execution;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

public final class MutantExecutor {

    private final ObjectMapper mapper = new ObjectMapper();

    public ExecutionResult execute(Map<String, String> sources, String testClassName, Duration timeout) {
        long timeoutNanos = timeout.toNanos();
        if (timeoutNanos <= 0) {
            throw new IllegalArgumentException("The worker timeout must be positive.");
        }

        JUnitWorker.Request request = new JUnitWorker.Request(sources, testClassName);
        Process process = startWorker();
        FutureTask<ExecutionResult> response = new FutureTask<>(() -> exchange(process, request));
        Thread exchangeThread = new Thread(response, "junit-worker-io");
        exchangeThread.setDaemon(true);

        try {
            long deadline = System.nanoTime() + timeoutNanos;
            exchangeThread.start();
            if (!process.waitFor(timeoutNanos, NANOSECONDS)) {
                return timedOut(timeout);
            }
            if (process.exitValue() != 0) {
                throw new IllegalStateException("JUnit worker exited with code " + process.exitValue());
            }
            return response.get(Math.max(0, deadline - System.nanoTime()), NANOSECONDS);
        } catch (TimeoutException exception) {
            return timedOut(timeout);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while awaiting the JUnit worker.", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Could not read the JUnit worker result.", exception.getCause());
        } finally {
            stopWorker(process);
            response.cancel(true);
        }
    }

    private static Process startWorker() {
        String executable = File.separatorChar == '\\' ? "java.exe" : "java";
        Path java = Path.of(System.getProperty("java.home"), "bin", executable);
        try {
            return new ProcessBuilder(java.toString(), "-cp", WorkerClasspath.workerClassPath(),
                    JUnitWorker.class.getName())
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not start the JUnit worker.", exception);
        }
    }

    private ExecutionResult exchange(Process process, JUnitWorker.Request request) throws IOException {
        try (OutputStream input = process.getOutputStream()) {
            mapper.writeValue(input, request);
        }
        try (InputStream output = process.getInputStream()) {
            return mapper.readValue(output, ExecutionResult.class);
        }
    }

    private static ExecutionResult timedOut(Duration timeout) {
        return ExecutionResult.withoutTests(MutantStatus.TIMED_OUT,
                List.of("JUnit worker exceeded the time limit of " + timeout.toMillis() + " ms."));
    }

    private static void stopWorker(Process process) {
        if (!process.isAlive()) {
            return;
        }
        process.destroyForcibly();
        try {
            process.waitFor(1, SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
