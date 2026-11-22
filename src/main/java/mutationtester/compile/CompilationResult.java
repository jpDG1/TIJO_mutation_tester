package mutationtester.compile;

import java.util.List;
import java.util.Map;

public final class CompilationResult {

    private final boolean successful;
    private final Map<String, byte[]> classBytes;
    private final List<String> diagnostics;

    CompilationResult(boolean successful, Map<String, byte[]> classBytes, List<String> diagnostics) {
        this.successful = successful;
        this.classBytes = Map.copyOf(classBytes);
        this.diagnostics = List.copyOf(diagnostics);
    }

    public boolean successful() {
        return successful;
    }

    public List<String> diagnostics() {
        return diagnostics;
    }

    public InMemoryClassLoader createClassLoader(ClassLoader parent) {
        if (!successful) {
            throw new IllegalStateException("Cannot load classes from a failed compilation.");
        }
        return new InMemoryClassLoader(parent, classBytes);
    }
}
