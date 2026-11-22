package mutationtester.compile;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

final class InMemoryFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {

    private final Map<String, ByteArrayOutputStream> compiledClasses = new HashMap<>();

    InMemoryFileManager(StandardJavaFileManager fileManager) {
        super(fileManager);
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind, FileObject sibling)
            throws IOException {
        if (kind != Kind.CLASS) {
            throw new IOException("Unsupported compiler output: " + kind);
        }

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        compiledClasses.put(className, bytes);
        URI uri = URI.create("memory:///" + className.replace('.', '/') + kind.extension);
        return new SimpleJavaFileObject(uri, kind) {
            @Override
            public OutputStream openOutputStream() {
                return bytes;
            }
        };
    }

    Map<String, byte[]> classBytes() {
        Map<String, byte[]> result = new HashMap<>();
        compiledClasses.forEach((className, bytes) -> result.put(className, bytes.toByteArray()));
        return result;
    }
}
