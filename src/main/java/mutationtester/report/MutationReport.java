package mutationtester.report;

import mutationtester.execution.MutantStatus;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static mutationtester.execution.MutantStatus.INVALID;
import static mutationtester.execution.MutantStatus.KILLED;
import static mutationtester.execution.MutantStatus.SURVIVED;
import static mutationtester.execution.MutantStatus.TIMED_OUT;

public record MutationReport(
        String sourceCode,
        double mutationScore,
        int killed,
        int timedOut,
        int survived,
        int invalid,
        List<MutantResult> mutants,
        List<Integer> survivingLines
) {

    public MutationReport {
        mutants = List.copyOf(mutants);
        survivingLines = List.copyOf(survivingLines);
    }

    public static MutationReport create(String sourceCode, List<MutantResult> results) {
        List<MutantResult> mutants = List.copyOf(results);
        Map<MutantStatus, Integer> counts = new EnumMap<>(MutantStatus.class);
        for (MutantResult mutant : mutants) {
            counts.merge(mutant.status(), 1, Integer::sum);
        }
        List<Integer> survivingLines = mutants.stream()
                .filter(mutant -> mutant.status() == SURVIVED)
                .map(MutantResult::line)
                .distinct()
                .sorted()
                .toList();

        return new MutationReport(sourceCode, ScoreCalculator.calculate(counts), counts.getOrDefault(KILLED, 0),
                counts.getOrDefault(TIMED_OUT, 0), counts.getOrDefault(SURVIVED, 0), counts.getOrDefault(INVALID, 0),
                mutants, survivingLines);
    }
}
