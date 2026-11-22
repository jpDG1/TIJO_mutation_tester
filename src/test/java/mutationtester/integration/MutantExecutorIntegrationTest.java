package mutationtester.integration;

import mutationtester.execution.ExecutionResult;
import mutationtester.execution.MutantExecutor;
import mutationtester.execution.MutantStatus;
import mutationtester.mutation.ConditionalBoundaryMutator;
import mutationtester.mutation.Mutant;
import mutationtester.parser.JavaSourceParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import static mutationtester.integration.PriceCalculatorSamples.SOURCE_CLASS_NAME;
import static mutationtester.integration.PriceCalculatorSamples.TEST_CLASS_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MutantExecutorIntegrationTest {

    private static final Duration RUN_LIMIT = Duration.ofSeconds(15);

    private final MutantExecutor executor = new MutantExecutor();

    @Test
    void reportsSurvivedWhenAllTestsPassInTheWorker() throws IOException {
        // Arrange
        Map<String, String> sources = PriceCalculatorSamples.sources();

        // Act
        ExecutionResult result = executor.execute(sources, TEST_CLASS_NAME, RUN_LIMIT);

        // Assert
        assertEquals(MutantStatus.SURVIVED, result.status());
        assertEquals(4, result.testRun().testsSucceeded());
    }

    @Test
    void reportsKilledWhenATestFailsInTheWorker() throws IOException {
        // Arrange
        Map<String, String> sources = changePriceCalculator("price * quantity", "price / quantity");

        // Act
        ExecutionResult result = executor.execute(sources, TEST_CLASS_NAME, RUN_LIMIT);

        // Assert
        assertEquals(MutantStatus.KILLED, result.status());
        assertEquals(1, result.testRun().testsFailed());
        assertTrue(result.testRun().failures().stream().anyMatch(failure -> failure.contains("returnsTotalForQuantity")));
    }

    @Test
    void reportsInvalidWhenTheMutantDoesNotCompile() throws IOException {
        // Arrange
        Map<String, String> sources = changePriceCalculator("price * quantity", "price * missingQuantity");

        // Act
        ExecutionResult result = executor.execute(sources, TEST_CLASS_NAME, RUN_LIMIT);

        // Assert
        assertEquals(MutantStatus.INVALID, result.status());
        assertTrue(result.diagnostics().stream().anyMatch(message -> message.contains("missingQuantity")));
    }

    @Test
    void timesOutAnInfiniteLoopMutant() throws IOException {
        // Arrange
        String sourceClass = "samples.InfiniteLoopSource";
        String testClass = "samples.InfiniteLoopTest";
        String source = SampleSources.read("InfiniteLoopSource.java");
        String tests = SampleSources.read("InfiniteLoopTest.java");
        ExecutionResult baseline = executor.execute(Map.of(sourceClass, source, testClass, tests), testClass, RUN_LIMIT);
        assertTrue(baseline.testRun().allTestsPassed(), baseline::toString);
        Mutant mutant = new ConditionalBoundaryMutator().generateMutants(new JavaSourceParser().parse(source)).get(0);
        Map<String, String> sources = Map.of(sourceClass, mutant.sourceCode(), testClass, tests);

        // Act
        ExecutionResult result = executor.execute(sources, testClass, Duration.ofSeconds(4));

        // Assert
        assertEquals(MutantStatus.TIMED_OUT, result.status());
    }

    private static Map<String, String> changePriceCalculator(String original, String replacement) throws IOException {
        Map<String, String> sources = PriceCalculatorSamples.sources();
        return Map.of(SOURCE_CLASS_NAME, sources.get(SOURCE_CLASS_NAME).replace(original, replacement),
                TEST_CLASS_NAME, sources.get(TEST_CLASS_NAME));
    }
}
