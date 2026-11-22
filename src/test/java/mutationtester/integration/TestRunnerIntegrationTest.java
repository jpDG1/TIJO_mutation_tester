package mutationtester.integration;

import com.github.javaparser.ast.CompilationUnit;
import mutationtester.compile.CompilationResult;
import mutationtester.execution.TestRunResult;
import mutationtester.execution.TestRunner;
import mutationtester.mutation.ArithmeticMutator;
import mutationtester.mutation.Mutant;
import mutationtester.parser.JavaSourceParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

import static mutationtester.integration.PriceCalculatorSamples.SOURCE_CLASS_NAME;
import static mutationtester.integration.PriceCalculatorSamples.TEST_CLASS_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestRunnerIntegrationTest {

    private final TestRunner runner = new TestRunner();

    @Test
    void runsAllJUnitTestsOnTheOriginalClass() throws IOException, URISyntaxException, ClassNotFoundException {
        // Arrange
        Class<?> testClass = compileAndLoad(PriceCalculatorSamples.sources(), TEST_CLASS_NAME);
        ClassLoader previousLoader = Thread.currentThread().getContextClassLoader();

        // Act
        TestRunResult result = runner.runTests(testClass);

        // Assert
        assertEquals(4, result.testsFound());
        assertEquals(4, result.testsSucceeded());
        assertTrue(result.allTestsPassed());
        assertSame(previousLoader, Thread.currentThread().getContextClassLoader());
    }

    @Test
    void reportsTheTestThatDetectsAMutant() throws IOException, URISyntaxException, ClassNotFoundException {
        // Arrange
        Map<String, String> originalSources = PriceCalculatorSamples.sources();
        CompilationUnit original = new JavaSourceParser().parse(originalSources.get(SOURCE_CLASS_NAME));
        Mutant mutant = new ArithmeticMutator().generateMutants(original).stream()
                .filter(candidate -> candidate.originalToken().equals("*"))
                .findFirst()
                .orElseThrow();
        Map<String, String> mutantSources = Map.of(
                SOURCE_CLASS_NAME, mutant.sourceCode(),
                TEST_CLASS_NAME, originalSources.get(TEST_CLASS_NAME)
        );
        Class<?> testClass = compileAndLoad(mutantSources, TEST_CLASS_NAME);

        // Act
        TestRunResult result = runner.runTests(testClass);

        // Assert
        assertEquals(4, result.testsFound());
        assertEquals(3, result.testsSucceeded());
        assertEquals(1, result.testsFailed());
        assertFalse(result.allTestsPassed());
        assertTrue(result.failures().stream().anyMatch(failure -> failure.contains("returnsTotalForQuantity")));
    }

    @Test
    void reportsZeroTestsForAClassWithoutJUnitTests() throws IOException, URISyntaxException, ClassNotFoundException {
        // Arrange
        Class<?> sourceClass = compileAndLoad(PriceCalculatorSamples.sources(), SOURCE_CLASS_NAME);

        // Act
        TestRunResult result = runner.runTests(sourceClass);

        // Assert
        assertEquals(0, result.testsFound());
        assertFalse(result.allTestsPassed());
    }

    private static Class<?> compileAndLoad(Map<String, String> sources, String className)
            throws URISyntaxException, ClassNotFoundException {
        CompilationResult compilation = PriceCalculatorSamples.junitCompiler().compile(sources);
        assertTrue(compilation.successful(), compilation.diagnostics()::toString);
        return compilation.createClassLoader(TestRunnerIntegrationTest.class.getClassLoader()).loadClass(className);
    }
}
