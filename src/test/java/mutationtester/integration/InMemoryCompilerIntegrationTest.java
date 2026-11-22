package mutationtester.integration;

import mutationtester.compile.CompilationResult;
import mutationtester.compile.InMemoryCompiler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryCompilerIntegrationTest {

    @Test
    void compilesAndLoadsTheSampleClassAndJUnitTests()
            throws IOException, URISyntaxException, ReflectiveOperationException {
        // Arrange
        InMemoryCompiler compiler = PriceCalculatorSamples.junitCompiler();
        Map<String, String> sources = PriceCalculatorSamples.sources();

        // Act
        CompilationResult result = compiler.compile(sources);

        // Assert
        assertTrue(result.successful(), result.diagnostics()::toString);
        ClassLoader loader = result.createClassLoader(getClass().getClassLoader());
        assertSame(loader, loader.loadClass(PriceCalculatorSamples.SOURCE_CLASS_NAME).getClassLoader());
        Class<?> testClass = loader.loadClass(PriceCalculatorSamples.TEST_CLASS_NAME);
        assertSame(loader, testClass.getClassLoader());
        assertTrue(testClass.getDeclaredMethod("returnsTotalForQuantity").isAnnotationPresent(Test.class));
        assertTrue(testClass.getDeclaredMethod("checksDiscountAtBoundary", int.class, boolean.class)
                .isAnnotationPresent(ParameterizedTest.class));
    }

    @Test
    void isolatesClassVersionsWithTheSameName() throws ReflectiveOperationException {
        // Arrange
        String originalSource = """
                public class ArithmeticValue {
                    public int value() {
                        return new Calculation().value();
                    }

                    private static class Calculation {
                        int value() {
                            return 5 + 2;
                        }
                    }
                }
                """;
        String mutantSource = originalSource.replace("5 + 2", "5 - 2");
        InMemoryCompiler compiler = new InMemoryCompiler();
        CompilationResult original = compiler.compile(Map.of("ArithmeticValue", originalSource));
        CompilationResult mutant = compiler.compile(Map.of("ArithmeticValue", mutantSource));
        ClassLoader originalLoader = original.createClassLoader(getClass().getClassLoader());
        ClassLoader mutantLoader = mutant.createClassLoader(originalLoader);

        // Act
        Class<?> originalClass = originalLoader.loadClass("ArithmeticValue");
        Class<?> mutantClass = mutantLoader.loadClass("ArithmeticValue");
        int originalValue = readValue(originalClass);
        int mutantValue = readValue(mutantClass);

        // Assert
        assertNotSame(originalClass, mutantClass);
        assertEquals(7, originalValue);
        assertEquals(3, mutantValue);
    }

    @Test
    void returnsSourceLocationWhenCompilationFails() {
        // Arrange
        InMemoryCompiler compiler = new InMemoryCompiler();
        String source = """
                class Broken {
                    int value() {
                        return missingValue;
                    }
                }
                """;

        // Act
        CompilationResult result = compiler.compile(Map.of("Broken", source));

        // Assert
        assertFalse(result.successful());
        assertTrue(result.diagnostics().stream()
                .anyMatch(message -> message.contains("Broken.java:3:") && message.contains("missingValue")),
                result.diagnostics()::toString);
        assertThrows(IllegalStateException.class, () -> result.createClassLoader(getClass().getClassLoader()));
    }

    @Test
    void rejectsJdkApisAddedAfterJava17() {
        // Arrange
        InMemoryCompiler compiler = new InMemoryCompiler();
        String source = """
                import java.util.List;

                class VersionCheck {
                    String first(List<String> values) {
                        return values.getFirst();
                    }
                }
                """;

        // Act
        CompilationResult result = compiler.compile(Map.of("VersionCheck", source));

        // Assert
        assertFalse(result.successful());
        assertTrue(result.diagnostics().stream().anyMatch(message -> message.contains("getFirst")),
                result.diagnostics()::toString);
    }

    private static int readValue(Class<?> type) throws ReflectiveOperationException {
        Object instance = type.getDeclaredConstructor().newInstance();
        return (int) type.getMethod("value").invoke(instance);
    }
}
