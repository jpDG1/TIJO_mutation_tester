package mutationtester.integration;

import mutationtester.compile.InMemoryCompiler;
import org.apiguardian.api.API;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.platform.commons.annotation.Testable;
import org.opentest4j.AssertionFailedError;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

final class PriceCalculatorSamples {

    static final String SOURCE_CLASS_NAME = "samples.PriceCalculator";
    static final String TEST_CLASS_NAME = "samples.PriceCalculatorStrongTest";

    private PriceCalculatorSamples() {
    }

    static Map<String, String> sources() throws IOException {
        return Map.of(
                SOURCE_CLASS_NAME, SampleSources.read("PriceCalculator.java"),
                TEST_CLASS_NAME, SampleSources.read("PriceCalculatorStrongTest.java")
        );
    }

    static InMemoryCompiler junitCompiler() throws URISyntaxException {
        return new InMemoryCompiler(List.of(
                locationOf(Test.class), locationOf(ParameterizedTest.class), locationOf(Testable.class),
                locationOf(API.class), locationOf(AssertionFailedError.class)
        ));
    }

    private static Path locationOf(Class<?> type) throws URISyntaxException {
        return Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI());
    }
}
