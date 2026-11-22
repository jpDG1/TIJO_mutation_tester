package mutationtester.report;

import mutationtester.execution.MutantStatus;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static mutationtester.execution.MutantStatus.INVALID;
import static mutationtester.execution.MutantStatus.KILLED;
import static mutationtester.execution.MutantStatus.SURVIVED;
import static mutationtester.execution.MutantStatus.TIMED_OUT;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ScoreCalculatorTest {

    @Test
    void calculatesThePercentageOfKilledMutants() {
        // Arrange
        Map<MutantStatus, Integer> counts = Map.of(KILLED, 5, SURVIVED, 7);

        // Act
        double score = ScoreCalculator.calculate(counts);

        // Assert
        assertEquals(41.6666667, score, 0.000001);
    }

    @Test
    void countsTimeoutsAsKilledAndExcludesInvalidMutants() {
        // Arrange
        Map<MutantStatus, Integer> counts = Map.of(KILLED, 1, TIMED_OUT, 2, SURVIVED, 1, INVALID, 4);

        // Act
        double score = ScoreCalculator.calculate(counts);

        // Assert
        assertEquals(75.0, score);
    }

    @Test
    void returnsZeroWhenEveryMutantIsInvalid() {
        // Arrange
        Map<MutantStatus, Integer> counts = Map.of(INVALID, 3);

        // Act
        double score = ScoreCalculator.calculate(counts);

        // Assert
        assertEquals(0.0, score);
    }
}
