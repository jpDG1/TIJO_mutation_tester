package mutationtester.compile;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class InMemoryCompiler {

    private static final List<String> OPTIONS = List.of("--release", "17", "-proc:none");

    private final List<Path> classPath;

    public InMemoryCompiler() {
        this(List.of());
    }

    public InMemoryCompiler(List<Path> classPath) {
        this.classPath = List.copyOf(classPath);
    }

    /** Compiles sources together using their fully qualified class names as keys. */
    public CompilationResult compile(Map<String, String> sources) {
        if (sources.isEmpty()) {
            throw new IllegalArgumentException("At least one Java source is required.");
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("A JDK with a Java compiler is required.");
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        List<SourceFile> sourceFiles = sources.entrySet().stream()
                .map(entry -> new SourceFile(entry.getKey(), entry.getValue()))
                .toList();

        try (StandardJavaFileManager standardFiles = compiler.getStandardFileManager(
                diagnostics, Locale.ROOT, StandardCharsets.UTF_8)) {
            standardFiles.setLocationFromPaths(StandardLocation.CLASS_PATH, classPath);
            InMemoryFileManager files = new InMemoryFileManager(standardFiles);
            boolean successful = compiler.getTask(null, files, diagnostics, OPTIONS, null, sourceFiles).call();
            List<String> messages = diagnostics.getDiagnostics().stream()
                    .map(InMemoryCompiler::formatDiagnostic)
                    .toList();
            return new CompilationResult(successful, successful ? files.classBytes() : Map.of(), messages);
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not access compiler files.", exception);
        }
    }

    private static String formatDiagnostic(Diagnostic<? extends JavaFileObject> diagnostic) {
        String message = diagnostic.getMessage(Locale.ROOT);
        JavaFileObject source = diagnostic.getSource();
        if (source == null || diagnostic.getLineNumber() == Diagnostic.NOPOS) {
            return message;
        }
        return "%s:%d:%d: %s".formatted(
                source.getName(), diagnostic.getLineNumber(), diagnostic.getColumnNumber(), message);
    }

    private static final class SourceFile extends SimpleJavaFileObject {

        private final String source;

        private SourceFile(String className, String source) {
            super(URI.create("memory:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.source = source;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }
}
