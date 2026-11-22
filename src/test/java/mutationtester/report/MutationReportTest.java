package mutationtester.report;

import mutationtester.mutation.Mutant;
import org.junit.jupiter.api.Test;

import java.util.List;

import static mutationtester.execution.MutantStatus.INVALID;
import static mutationtester.execution.MutantStatus.KILLED;
import static mutationtester.execution.MutantStatus.SURVIVED;
import static mutationtester.execution.MutantStatus.TIMED_OUT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MutationReportTest {

    @Test
    void reportsStatusCountsAndDistinctSurvivingLines() {
        // Arrange
        String source = """
                class Calculator {
                    int value(int left, int right) {
                        int sum = left + right;
                        int scaled = (left * 2) + (right - 1);
                        return sum - scaled + right;
                    }
                }
                """;
        Mutant mutant = new Mutant("ArithmeticMutator", 5, "-", "+", source.replace("sum - scaled", "sum + scaled"));
        List<MutantResult> results = List.of(
                MutantResult.from(1, mutant, SURVIVED),
                new MutantResult(2, "ArithmeticMutator", 3, "Replaced + with -", KILLED),
                new MutantResult(3, "ArithmeticMutator", 4, "Replaced * with /", SURVIVED),
                new MutantResult(4, "ArithmeticMutator", 5, "Replaced + with -", SURVIVED),
                new MutantResult(5, "ArithmeticMutator", 4, "Replaced + with -", TIMED_OUT),
                new MutantResult(6, "ArithmeticMutator", 4, "Replaced - with +", INVALID)
        );

        // Act
        MutationReport report = MutationReport.create(source, results);

        // Assert
        assertEquals(40.0, report.mutationScore());
        assertEquals(1, report.killed());
        assertEquals(1, report.timedOut());
        assertEquals(3, report.survived());
        assertEquals(1, report.invalid());
        assertEquals(source, report.sourceCode());
        assertEquals(results, report.mutants());
        assertEquals(new MutantResult(1, "ArithmeticMutator", 5, "Replaced - with +", SURVIVED), report.mutants().get(0));
        assertEquals(List.of(4, 5), report.survivingLines());
    }

    @Test
    void createsAZeroScoreAndEmptyHighlightsWhenThereAreNoMutants() {
        // Arrange
        String source = "class Empty {}";
        List<MutantResult> results = List.of();

        // Act
        MutationReport report = MutationReport.create(source, results);

        // Assert
        assertEquals(0.0, report.mutationScore());
        assertTrue(report.mutants().isEmpty());
        assertTrue(report.survivingLines().isEmpty());
    }
}
