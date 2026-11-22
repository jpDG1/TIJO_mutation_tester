package mutationtester.integration;

import mutationtester.progress.RunProgress;
import mutationtester.service.AnalysisRejectedException;
import mutationtester.service.MutationRun;
import mutationtester.service.MutationRunService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static mutationtester.integration.PriceCalculatorSamples.SOURCE_CLASS_NAME;
import static mutationtester.integration.PriceCalculatorSamples.TEST_CLASS_NAME;
import static mutationtester.service.RunStatus.COMPLETED;
import static mutationtester.service.RunStatus.RUNNING;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class MutationRunServiceIntegrationTest {

    private final MutationRunService service;

    @Autowired
    MutationRunServiceIntegrationTest(MutationRunService service) {
        this.service = service;
    }

    @Test
    void completesAnalysisAndReportsEveryMutant() throws IOException {
        // Arrange
        Map<String, String> sources = PriceCalculatorSamples.sources();

        // Act
        UUID runId = service.startRun(sources.get(SOURCE_CLASS_NAME), sources.get(TEST_CLASS_NAME));
        await().atMost(Duration.ofSeconds(30))
                .until(() -> service.findRun(runId).orElseThrow().status() != RUNNING);
        MutationRun run = service.findRun(runId).orElseThrow();

        // Assert
        assertEquals(COMPLETED, run.status(), run.error());
        assertEquals(new RunProgress(5, 5), run.progress());
        assertEquals(5, run.report().killed());
        assertEquals(100.0, run.report().mutationScore());
        assertTrue(run.report().survivingLines().isEmpty());
    }

    @Test
    void rejectsAnalysisWhenAnOriginalTestFails() throws IOException {
        // Arrange
        Map<String, String> sources = PriceCalculatorSamples.sources();
        String failingTests = sources.get(TEST_CLASS_NAME).replace("assertEquals(36, total)", "assertEquals(99, total)");

        // Act
        AnalysisRejectedException rejection = assertThrows(AnalysisRejectedException.class,
                () -> service.startRun(sources.get(SOURCE_CLASS_NAME), failingTests));

        // Assert
        assertEquals("All tests must pass on the original class.", rejection.getMessage());
        assertTrue(rejection.diagnostics().stream().anyMatch(message -> message.contains("returnsTotalForQuantity")));
    }
}
