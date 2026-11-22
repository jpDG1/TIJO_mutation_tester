package mutationtester.report;

import mutationtester.execution.MutantStatus;
import mutationtester.mutation.Mutant;

public record MutantResult(int id, String mutator, int line, String description, MutantStatus status) {

    public static MutantResult from(int id, Mutant mutant, MutantStatus status) {
        String description = "Replaced %s with %s".formatted(mutant.originalToken(), mutant.replacementToken());
        return new MutantResult(id, mutant.mutatorName(), mutant.lineNumber(), description, status);
    }
}
