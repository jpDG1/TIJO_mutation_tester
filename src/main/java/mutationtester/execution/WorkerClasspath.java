package mutationtester.execution;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apiguardian.api.API;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.platform.commons.annotation.Testable;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.launcher.Launcher;
import org.opentest4j.AssertionFailedError;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

final class WorkerClasspath {

    private WorkerClasspath() {
    }

    static List<Path> junitCompilationPaths() {
        return List.of(locationOf(Test.class), locationOf(ParameterizedTest.class), locationOf(Testable.class),
                locationOf(API.class), locationOf(AssertionFailedError.class));
    }

    static String workerClassPath() {
        List<Path> paths = new ArrayList<>(junitCompilationPaths());
        paths.addAll(List.of(
                locationOf(JUnitWorker.class), locationOf(Launcher.class), locationOf(TestEngine.class),
                locationOf(jupiterEngine()), locationOf(ObjectMapper.class),
                locationOf(JsonFactory.class), locationOf(JsonProperty.class)
        ));
        return paths.stream().distinct().map(Path::toString).collect(Collectors.joining(File.pathSeparator));
    }

    private static Class<?> jupiterEngine() {
        try {
            return Class.forName("org.junit.jupiter.engine.JupiterTestEngine", false,
                    WorkerClasspath.class.getClassLoader());
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("The JUnit Jupiter engine is required.", exception);
        }
    }

    private static Path locationOf(Class<?> type) {
        try {
            URI location = type.getProtectionDomain().getCodeSource().getLocation().toURI();
            if (!"file".equals(location.getScheme())) {
                throw new IllegalStateException("Start the app with Maven or the IDE to run JUnit workers.");
            }
            return Path.of(location);
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("Could not resolve the classpath for " + type.getName(), exception);
        }
    }
}
