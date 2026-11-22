package mutationtester.report;

import mutationtester.execution.MutantStatus;

import java.util.Map;

import static mutationtester.execution.MutantStatus.KILLED;
import static mutationtester.execution.MutantStatus.SURVIVED;
import static mutationtester.execution.MutantStatus.TIMED_OUT;

public final class ScoreCalculator {

    private ScoreCalculator() {
    }

    public static double calculate(Map<MutantStatus, Integer> counts) {
        long detected = (long) counts.getOrDefault(KILLED, 0) + counts.getOrDefault(TIMED_OUT, 0);
        long valid = detected + counts.getOrDefault(SURVIVED, 0);
        return valid == 0 ? 0.0 : 100.0 * detected / valid;
    }
}
