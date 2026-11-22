package mutationtester.execution;

import com.fasterxml.jackson.databind.ObjectMapper;
import mutationtester.compile.CompilationResult;
import mutationtester.compile.InMemoryCompiler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Objects;

public final class JUnitWorker {

    private JUnitWorker() {
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        OutputStream response = System.out;
        System.setOut(System.err);

        ObjectMapper mapper = new ObjectMapper();
        Request request = mapper.readValue(System.in, Request.class);
        mapper.writeValue(response, execute(request));
    }

    private static ExecutionResult execute(Request request) throws ClassNotFoundException {
        InMemoryCompiler compiler = new InMemoryCompiler(WorkerClasspath.junitCompilationPaths());
        CompilationResult compilation = compiler.compile(request.sources());
        if (!compilation.successful()) {
            return ExecutionResult.withoutTests(MutantStatus.INVALID, compilation.diagnostics());
        }

        ClassLoader loader = compilation.createClassLoader(JUnitWorker.class.getClassLoader());
        Class<?> testClass = loader.loadClass(request.testClassName());
        TestRunResult testRun = new TestRunner().runTests(testClass);
        MutantStatus status = testRun.failures().isEmpty() ? MutantStatus.SURVIVED : MutantStatus.KILLED;
        return new ExecutionResult(status, testRun, compilation.diagnostics());
    }

    record Request(Map<String, String> sources, String testClassName) {

        Request {
            sources = Map.copyOf(sources);
            Objects.requireNonNull(testClassName);
        }
    }
}
